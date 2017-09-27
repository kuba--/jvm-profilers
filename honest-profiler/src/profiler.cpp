#include "profiler.h"
#include <sys/time.h>

ASGCTType Asgct::asgct_;

bool Profiler::start(JNIEnv *jniEnv) {
    // reference back to Profiler::handle on the singleton
    // instance of Profiler
    handler_.SetAction(&bootstrapHandle);
    processor->start(jniEnv);
    return handler_.updateSigprofInterval();
}

void Profiler::stop() {
    handler_.stopSigprof();
    processor->stop();
    signal(SIGPROF, SIG_IGN);
}

void Profiler::handle(int signum, siginfo_t *info, void *context) {
    IMPLICITLY_USE(signum);
    IMPLICITLY_USE(info);

    // prepare sample data structure
    JVMPI_CallFrame frames[kMaxFramesToCapture];
    safe_reset(frames, sizeof(JVMPI_CallFrame) * kMaxFramesToCapture);

    JVMPI_CallTrace trace;
    trace.frames = frames;
    JNIEnv *jniEnv = getJNIEnv();
    if (jniEnv == NULL) {
        trace.num_frames = -3; // ticks_unknown_not_Java
    } else {
        trace.env_id = jniEnv;
        ASGCTType asgct = Asgct::GetAsgct();
          (*asgct)(&trace, kMaxFramesToCapture, context);
    }
    // log all samples, failures included, let the post processing sift through the data
    buffer->push(trace);
}

bool Profiler::isRunning() const {
    return processor->isRunning();
}

JNIEnv *Profiler::getJNIEnv() {
    JNIEnv *jniEnv = NULL;
    int getEnvStat = jvm_->GetEnv((void **)&jniEnv, JNI_VERSION_1_6);
    // check for issues
    if (getEnvStat == JNI_EDETACHED || getEnvStat == JNI_EVERSION) {
        jniEnv = NULL;
    }
    return jniEnv;
}

bool Profiler::lookupFrameInformation(const JVMPI_CallFrame &frame, jvmtiEnv *jvmti, MethodListener &logWriter) {
    jint error;
    JvmtiScopedPtr<char> methodName(jvmti);

    error = jvmti->GetMethodName(frame.method_id, methodName.GetRef(), NULL, NULL);
    if (error != JVMTI_ERROR_NONE) {
        methodName.AbandonBecauseOfError();
        if (error == JVMTI_ERROR_INVALID_METHODID) {
            static int once = 0;
            if (!once) {
                once = 1;
                logError("One of your monitoring interfaces "
                         "is having trouble resolving its stack traces.  "
                         "GetMethodName on a jmethodID involved in a stacktrace "
                         "resulted in an INVALID_METHODID error which usually "
                         "indicates its declaring class has been unloaded.\n");
                logError("Unexpected JVMTI error %d in GetMethodName", error);
            }
        }
        return false;
    }

    // Get class name, put it in signature_ptr
    jclass declaring_class;
    JVMTI_ERROR_1(jvmti->GetMethodDeclaringClass(frame.method_id, &declaring_class), false);

    JvmtiScopedPtr<char> signature_ptr2(jvmti);
    JVMTI_ERROR_CLEANUP_1(jvmti->GetClassSignature(declaring_class, signature_ptr2.GetRef(), NULL), false, signature_ptr2.AbandonBecauseOfError());

    // Get source file, put it in source_name_ptr
    char *fileName;
    JvmtiScopedPtr<char> source_name_ptr(jvmti);
    static char file_unknown[] = "UnknownFile";
    if (JVMTI_ERROR_NONE != jvmti->GetSourceFileName(declaring_class, source_name_ptr.GetRef())) {
        source_name_ptr.AbandonBecauseOfError();
        fileName = file_unknown;
    } else {
        fileName = source_name_ptr.Get();
    }

    logWriter.recordNewMethod((method_id) frame.method_id, fileName,
            signature_ptr2.Get(), methodName.Get());

    return true;
}
