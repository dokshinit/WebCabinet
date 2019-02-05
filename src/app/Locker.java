/*
 * Copyright (c) 2013, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package app;

import java.util.Date;

/**
 * Простой локер с синхронизацией. Служит для блокировки асинхронного доступа к
 * чему-либо.
 *
 * <pre> Locker locker = new Locker();
 * ...
 * Object stamp = locker.lock();
 * if (stamp != null) {
 *   ... содержимое выполняемое всегда синхронизированно ...
 * }
 * locker.unlock(stamp);
 * </pre>
 *
 *
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class Locker {

    private Date owner;
    private boolean isLocked;

    public synchronized boolean isLocked() {
        return isLocked;
    }

    public synchronized Object lock() {
        if (isLocked) {
            return null;
        }
        isLocked = true;
        return owner = new Date();
    }

    public synchronized void unlock(Object owner) {
        if (this.owner == owner) {
            this.isLocked = false;
            this.owner = null;
        }
    }
}
