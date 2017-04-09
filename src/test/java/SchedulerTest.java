import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static java.lang.Thread.sleep;

public class SchedulerTest
{
	// Время, которое мы даём ExecutorService на завершение своих дел
	private static final int EXECUTOR_TERMINATION_TIMEOUT_SECONDS = 5;
	private CopyOnWriteArrayList<Integer> taskLog;

	/**
	 * Тест порядка выполнения заранее отправленных заданий
	 */
	@Test
	public void testBasicSequence() throws InterruptedException
	{
		taskLog = new CopyOnWriteArrayList<>();
		Scheduler scheduler = new Scheduler();

		ExecutorService executorService = Executors.newCachedThreadPool();
		executorService.submit(scheduler);

		LocalDateTime time = LocalDateTime.now().plusSeconds(1);
		scheduler.addTask(new SleepingTestTaskRecord(2000, 100, taskLog, 1));
		scheduler.addTask(new SleepingTestTaskRecord(-1000, 100, taskLog, 2));
		scheduler.addTask(new SleepingTestTaskRecord(time, 100, taskLog, 3));
		scheduler.addTask(new SleepingTestTaskRecord(time, 100, taskLog, 4));

		sleep(3000);

		Assert.assertArrayEquals(new Integer[]{2, 3, 4, 1}, taskLog.toArray());

		scheduler.stop();
		executorService.shutdown();
		Assert.assertTrue(executorService.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS));
	}

	/**
	 * Тест ситуации, когда новое задание приходит во время выполнения предыдущего и успевает устареть
	 */
	@Test
	public void testDelayedSequence() throws InterruptedException
	{
		taskLog = new CopyOnWriteArrayList<>();
		Scheduler scheduler = new Scheduler();

		ExecutorService executorService = Executors.newCachedThreadPool();
		executorService.submit(scheduler);

		LocalDateTime time = LocalDateTime.now().plusSeconds(3);

		scheduler.addTask(new SleepingTestTaskRecord(500, 2000, taskLog, 1));
		sleep(1000);
		scheduler.addTask(new SleepingTestTaskRecord(time, 1000, taskLog, 3));
		scheduler.addTask(new SleepingTestTaskRecord(-1000, 1000, taskLog, 2));
		sleep(1000);
		scheduler.addTask(new SleepingTestTaskRecord(time, 1000, taskLog, 4));


		sleep(6000);

		Assert.assertArrayEquals(new Integer[]{1, 2, 3, 4}, taskLog.toArray());

		scheduler.stop();
		executorService.shutdown();
		Assert.assertTrue(executorService.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS));
	}

	/**
	 * Один поставщик добавляет задания со случайным временем начала
	 */
	@Test
	public void singleProviderStressTest() throws InterruptedException
	{
		taskLog = new CopyOnWriteArrayList<>();
		Scheduler scheduler = new Scheduler();

		ExecutorService executorService = Executors.newCachedThreadPool();
		executorService.submit(scheduler);

		// Количество заданий
		final int SIZE = 1000;

		// Максимальная задержка в мс между добавлениями заданий
		final int DELAY = 8;

		// Диапазон времени начала задания в мс от текущего момента вперёд
		final int MS_FORWARD = 10000;

		// Диапазон времени начала задания в мс от текущего момента назад
		final int MS_BACKWARDS = 5000;

		Random random = new Random();
		for (int i = 0; i < SIZE; i++)
		{
			scheduler.addTask(new SleepingTestTaskRecord(random.nextInt(MS_FORWARD + MS_BACKWARDS) - MS_BACKWARDS, 0, taskLog, i));
			sleep(random.nextInt(DELAY));
		}

		sleep(MS_FORWARD + 1000);

		scheduler.log("Assert time");
		Assert.assertEquals(SIZE, taskLog.size());

		scheduler.stop();
		executorService.shutdown();
		Assert.assertTrue(executorService.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS));
	}

	/**
	 * Несколько поставщиков асинхронно добавляют задания со случайным временем начала
	 */
	@Test
	public void multiProviderStressTest() throws InterruptedException, ExecutionException
	{
		taskLog = new CopyOnWriteArrayList<>();
		final Scheduler scheduler = new Scheduler();

		ExecutorService executorService = Executors.newCachedThreadPool();
		executorService.submit(scheduler);

		// Количество заданий от каждого поставщика
		final int SIZE = 10000;

		// Количество поставщиков
		final int NUMBER_OF_PROVIDERS = 5;

		// Максимальная задержка в мс между добавлениями заданий
		final int DELAY = 6;

		// Диапазон времени начала задания в мс от текущего момента вперёд
		final int MS_FORWARD = 5000;

		// Диапазон времени начала задания в мс от текущего момента назад
		final int MS_BACKWARDS = 3000;

		Random random = new Random();
		List<Future> taskProviders = new ArrayList<>();
		for (int j = 0; j < NUMBER_OF_PROVIDERS; j++)
		{
			final int localIndex = j;
			taskProviders.add(executorService.submit(() ->
			{
				for (int i = 0; i < SIZE; i++)
				{
					scheduler.addTask(new SleepingTestTaskRecord(random.nextInt(MS_FORWARD + MS_BACKWARDS) - MS_BACKWARDS, 0, taskLog, i + localIndex * SIZE));
					sleep(random.nextInt(DELAY));
				}
				return null;
			}));
		}

		// Ждём завершение провайдеров
		for (Future taskProvider : taskProviders)
			taskProvider.get();

		sleep(MS_FORWARD + 1000);

		scheduler.log("Assert time");
		Assert.assertEquals(SIZE * NUMBER_OF_PROVIDERS, taskLog.size());

		scheduler.stop();
		executorService.shutdown();
		Assert.assertTrue(executorService.awaitTermination(EXECUTOR_TERMINATION_TIMEOUT_SECONDS, TimeUnit.SECONDS));
	}
}