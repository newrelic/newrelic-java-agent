package kotlinx.coroutines.scheduling;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "kotlinx.coroutines.scheduling.TasksKt")
public class TasksKt_Instrumentation {

    public static Task asTask(Runnable runnable, long submissionTime, boolean taskContext) {
        Task task = Weaver.callOriginal();
//        if(task instanceof TaskImpl_Instrumentation) {
//            TaskImpl_Instrumentation taskImpl = (TaskImpl_Instrumentation)task;
//            if(taskImpl.token == null) {
//                Token t = NewRelic.getAgent().getTransaction().getToken();
//                if(t != null) {
//                    if(t.isActive()) {
//                        taskImpl.token = t;
//                    } else {
//                        t.expire();
//                        t = null;
//                    }
//                }
//            }
//        }
        return task;
    }
}
