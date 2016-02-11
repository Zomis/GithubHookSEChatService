package net.zomis.duga

import grails.transaction.Transactional
import net.zomis.duga.tasks.CommentsScanTask
import net.zomis.duga.tasks.GithubTask
import net.zomis.duga.tasks.MessageTask
import net.zomis.duga.tasks.StatisticTask
import net.zomis.duga.tasks.UnansweredTask
import net.zomis.duga.tasks.UserRepDiffTask
import net.zomis.duga.tasks.qscan.QuestionScanTask
import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronTrigger

import java.util.concurrent.ScheduledFuture

@Transactional(readOnly = true)
class DugaTasks {

    private static final Logger logger = Logger.getLogger(DugaTasks)

    private final List<ScheduledFuture<?>> tasks = new ArrayList<>()
    private final List<TaskData> taskData = new ArrayList<TaskData>();

    @Autowired TaskScheduler scheduler

    def initOnce() {
        reloadAll()
    }

    List<TaskData> reloadAll() {
        List<TaskData> allTasks = TaskData.list()
        println 'Reloading tasks, contains ' + allTasks
        tasks.forEach({ScheduledFuture<?> task -> task.cancel(false) })
        tasks.clear()
        taskData.clear()
        taskData.addAll(allTasks)
        for (TaskData task : allTasks) {
            Runnable run = createTask(task.taskValue)
            ScheduledFuture<?> future = scheduler.schedule(new TaskRunner(task, run), new CronTrigger(task.cronStr, TimeZone.getTimeZone("UTC")))
            tasks.add(future)
            System.out.println("Added task: $task.taskValue - $run")
        }
        return allTasks
    }


    @Autowired private DugaBotService chatBot;
    @Autowired private GithubBean githubBean;
    @Autowired private StackExchangeAPI stackAPI;
    @Autowired private HookStringification stringification;

    private class TaskRunner implements Runnable {

        private final TaskData data;
        private final Runnable task;

        public TaskRunner(TaskData data, Runnable runnable) {
            this.data = data;
            this.task = runnable;
        }

        @Override
        public void run() {
            try {
                logger.info("Running task " + data);
                task.run();
                logger.info("Finished task " + data);
            } catch (Exception ex) {
                logger.error("Error running " + data, ex);
            }
        }
    }

    Runnable createTask(String taskData) {
        String[] taskInfo = taskData.split(";")
        switch (taskInfo[0]) {
            case "dailyStats":
                return new StatisticTask(chatBot, taskInfo[1])
            case "questionScan":
                return new QuestionScanTask(stackAPI, githubBean,
                        stringification, chatBot,
                        taskInfo[1], taskInfo[2], taskInfo[3])
            case "github":
                return new GithubTask(githubBean, stringification, chatBot)
            case "comments":
                return new CommentsScanTask(stackAPI, chatBot)
            case "mess":
                return new MessageTask(chatBot, taskInfo[1], taskInfo[2])
            case "ratingdiff":
                return new UserRepDiffTask(stackAPI, taskInfo[1], chatBot, taskInfo[2], taskInfo[3])
            case "unanswered":
                return new UnansweredTask(stackAPI, taskInfo[1], chatBot, taskInfo[2], taskInfo[3])
            default:
                return { println "Unknown task: $taskData" }
        }
    }

    public synchronized List<TaskData> getTasks() {
        return new ArrayList<>(taskData);
    }

}
