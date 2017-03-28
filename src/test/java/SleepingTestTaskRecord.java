package java;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Thread.sleep;

public class SleepingTestTaskRecord extends TaskRecord
{
	private int id;

	/**
	 * Тестовое задание: ждёт некоторое время, потом добавляет свой идентификатор в список (выполненного)
	 * @param time время начала
	 * @param workMS время эмулирования деятельности (мс)
	 * @param taskLog список, где отметиться по завершении
	 * @param id идентификатор (предположительно, уникальный)
	 */
	public SleepingTestTaskRecord(LocalDateTime time, long workMS, CopyOnWriteArrayList<Integer> taskLog, int id)
	{
		super(time, (() ->
		{
			if (workMS > 0)
				sleep(workMS);
			taskLog.add(id);
			return null;
		}));
		this.id = id;
	}

	/**
	 * Тестовое задание: ждёт некоторое время, потом добавляет свой идентификатор в список (выполненного)
	 * @param delayMS время начала работы относительно текущего момента (мс)
	 * @param workMS время эмулирования деятельности (мс)
	 * @param taskLog список, где отметиться по завершении
	 * @param id идентификатор (предположительно, уникальный)
	 */
	public SleepingTestTaskRecord(long delayMS, long workMS, CopyOnWriteArrayList<Integer> taskLog, int id)
	{
		this(LocalDateTime.now().plusNanos(delayMS * 1000000), workMS, taskLog, id);
	}

	public String toString()
	{
		return super.toString() + ", id = " + id;
	}
}
