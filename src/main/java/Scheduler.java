package java;/*
*   На вход поступают пары (LocalDateTime, Callable).
*   Нужно реализовать систему, которая будет выполнять Callable для каждого пришедшего события в указанный LocalDateTime.
*   Задачи должны выполняться в порядке согласно значению LocalDateTime.
*   Если на один момент времени указано более одной задачи, то порядок их выполнения определяется порядком поступления.
*   Если система перегружена, то задачи, время выполнения которых оказалось в прошлом, все равно должны выполниться согласно
*   приоритетам описанным выше.
*   Задачи могут приходить в произвольном порядке и из разных потоков.
*/

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;


/**
 * По формулировке не очень понятно, требуется ли выполнять задачи в одном потоке или в асинхронно в разных,
 * но поскольку упоминается перегрузка системы, видимо, предполагается, что в одном.
 */
public class Scheduler implements Callable<Object>
{
	// Пауза между обращениями к пустой очереди заданий.
	// Если начинать выполнение задания требуется с точностью до миллисекунды, можно поставить 0
	private static final int TIMEOUT = 0;

	// Для того, чтобы упростить вопросы синхронизации, не будем поддерживать сортировку списка задач по времени
	private CopyOnWriteArrayList<TaskRecord> tasksQueue;

	// Признак того, что пора заканчивать работу, можно установить извне
	private AtomicBoolean stop;

	public Scheduler()
	{
		tasksQueue = new CopyOnWriteArrayList<>();
		stop = new AtomicBoolean(true);
	}

	/**
	 * Чтобы не добавлять логгер в этот микро проект, но при этом представлять, что там происходит.
	 * Отмечаем также текущее время
	 * @param message сообщение
	 */
	public void log(String message)
	{
		synchronized (System.out)
		{
			System.out.println(LocalDateTime.now() + ": " + message);
		}
	}

	/**
	 * Просьба завершить процесс выполнения заданий
	 */
	public void stop()
	{
		stop.set(true);
	}

	/**
	 * Добавление нового задания
	 * @param task задание
	 */
	public void addTask(TaskRecord task)
	{
		tasksQueue.add(task);
		log("Added: " + task);
	}

	/**
	 * Поиск следующего задания для выполнения
	 * @return задание, которое требуется выполнять следующим, если такое есть
	 */
	private Optional<TaskRecord> popTask()
	{
		LocalDateTime now = LocalDateTime.now();

		Optional<TaskRecord> firstTask = tasksQueue.stream()
				.sequential()
				.filter(x -> x.getTime().isBefore(now))
				.reduce((x, y) -> y.getTime().isBefore(y.getTime()) ? y : x);

		firstTask.ifPresent(tasksQueue::remove);

		return firstTask;
	}

	/**
	 * Начать выполнение процесс выполнения заданий
	 * @return ничего
	 */
	@Override
	public Object call() throws Exception
	{
		stop.set(false);
		Optional<TaskRecord> currentTask;
		while (!stop.get())
		{
			currentTask = popTask();
			if (currentTask.isPresent())
			{
				log("Found task: " + currentTask.get());
				try
				{
					currentTask.get().getCallable().call();
				} catch (Exception e)
				{
					// По исключениям в отдельных задачах вываливаться не будем
					log("Log exception with any logger at hand: " + e);
				}
			}
			else
				sleep(TIMEOUT);
		}
		return null;
	}
}