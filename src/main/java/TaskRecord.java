import java.time.LocalDateTime;
import java.util.concurrent.Callable;

class TaskRecord
{
	private LocalDateTime time;
	private Callable callable;

	public TaskRecord(LocalDateTime time, Callable callable)
	{
		this.time = time;
		this.callable = callable;
	}

	public LocalDateTime getTime()
	{
		return time;
	}

	public Callable getCallable()
	{
		return callable;
	}

	public String toString()
	{
		return "task scheduled at " + time;
	}
}
