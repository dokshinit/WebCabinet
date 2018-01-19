/*
 * Copyright (c) 2015, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app;

import static app.AppServlet.logger;

/**
 * Задача для выполнения в своем отдельном потоке. Используется профайлер для оценки времени исполнения.
 *
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class Task implements Runnable {

    @FunctionalInterface
    public interface RunnableTask {
        void run(Task task);
    }

    private final String title;
    private final Runnable run;
    private final RunnableTask runtask;
    private final Thread thread;
    private boolean aborted; // флаг для досрочного прерывания задачи (исключает fireOnError, fireEnd...)

    public Task(String title, Runnable run) {
        this.title = title;
        this.run = run;
        this.runtask = null;
        this.thread = new Thread(this);
        this.aborted = false;
    }

    public Task(String title, RunnableTask runtask) {
        this.title = title;
        this.run = null;
        this.runtask = runtask;
        this.thread = new Thread(this);
        this.aborted = false;
    }

    public static Task start(String title, Runnable run) {
        Task task = new Task(title, run);
        task.start();
        return task;
    }

    public static Task start(String title, RunnableTask runtask) {
        Task task = new Task(title, runtask);
        task.start();
        return task;
    }

    public void start() {
        thread.start();
    }

    @Override
    public void run() {
        aborted = false;

        fireBeforeStart();
        if (!aborted) {
            logger.infof("[%s] Запуск задачи.", title);
            try {
                if (run != null) {
                    run.run();
                } else if (runtask != null) {
                    runtask.run(this);
                }

            } catch (Exception ex) {
                logger.errorf("[%s] Ошибка! %s:%s", title, ex.getClass().getName(), ex.getMessage());
                if (!aborted) fireOnRunError(ex);
            }
            logger.infof("[%s] Завершение задачи.", title);
            if (!aborted) fireAfterEnd();
        }
    }

    /** Выставляет флаг досрочного прерывания обработки. */
    public void abort() {
        aborted = true;
    }

    public boolean isAborted() { return aborted; }

    /** Вызывается перед выполнением задачи. */
    protected void fireBeforeStart() {
    }

    /** Вызывается при ошибке выполнения задачи. Если aborted=false! */
    protected void fireOnRunError(Exception ex) {
    }

    /** Вызывается после завершения задачи (даже при ошибках). Если aborted=false! */
    protected void fireAfterEnd() {
    }

    public String getTitle() {
        return title;
    }

    public Thread getThread() {
        return thread;
    }
}
