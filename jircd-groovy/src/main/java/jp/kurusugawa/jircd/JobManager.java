package jp.kurusugawa.jircd;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import jircd.irc.ConnectedEntity;
import jp.kurusugawa.jircd.IRCDaemon.Service;

import org.apache.log4j.Logger;

public class JobManager implements Service {
	private static final Logger LOG = Logger.getLogger(JobManager.class);

	private Queue<Job> mJobs;

	private ExecutorService mExecutor;

	public void execute(Job aJob) {
		mJobs.add(aJob);
		mExecutor.execute(aJob);
	}

	@SuppressWarnings("deprecation")
	public void cancel(int aJobId) {
		for (Job tJob : mJobs) {
			if (System.identityHashCode(tJob) != aJobId) {
				continue;
			}
			tJob.getThread().stop();
			return;
		}
		throw new RuntimeException("no such job id(=" + aJobId + ")");
	}

	public static abstract class Job implements Runnable {
		private final ConnectedEntity mOwner;
		private Status mStatus;

		private final long mCreateTime;
		private long mStartTime;
		private long mEndTime;
		private Thread mThread;

		public enum Status {
			WAIT, RUNNING, SUCCESS, FAILURE, DONE
		};

		protected Job(ConnectedEntity aOwner) {
			mOwner = aOwner;
			mStatus = Status.WAIT;
			mCreateTime = System.currentTimeMillis();
		}

		protected abstract void process() throws Throwable;

		public void run() {
			try {
				mStatus = Status.RUNNING;
				mThread = Thread.currentThread();
				mStartTime = System.currentTimeMillis();
				process();
				mStatus = Status.SUCCESS;
			} catch (Throwable t) {
				mStatus = Status.FAILURE;
				LOG.warn("", t);
			} finally {
				mThread = null;
				mEndTime = System.currentTimeMillis();
			}
		}

		@Override
		public String toString() {
			StringBuilder tBuffer = new StringBuilder();
			switch (mStatus) {
			case WAIT:
				tBuffer.append("W");
				break;
			case RUNNING:
				tBuffer.append("R");
				break;
			case SUCCESS:
				tBuffer.append("S");
				break;
			case FAILURE:
				tBuffer.append("F");
				break;
			case DONE:
				tBuffer.append("D");
				break;
			default:
				tBuffer.append("?");
				break;
			}
			tBuffer.append('\t');
			tBuffer.append(mOwner.getName());
			tBuffer.append('\t');
			tBuffer.append(System.identityHashCode(this));
			tBuffer.append('\t');
			tBuffer.append(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS").format(new Date(mStartTime)));
			tBuffer.append('\t');
			tBuffer.append(System.currentTimeMillis() - mStartTime);
			return new String(tBuffer);
		}

		public ConnectedEntity getOwner() {
			return mOwner;
		}

		public Status getStatus() {
			return mStatus;
		}

		public long getCreateTime() {
			return mCreateTime;
		}

		public long getStartTime() {
			return mStartTime;
		}

		public long getEndTime() {
			return mEndTime;
		}

		public Thread getThread() {
			return mThread;
		}
	}

	@Override
	public String toString() {
		StringBuilder tBuilder = new StringBuilder(super.toString());
		tBuilder.append(mJobs.toString());
		return new String(tBuilder);
	}

	public void startup(Properties aSettings) {
		int tCorePoolSize = Integer.parseInt(aSettings.getProperty("job.executor.corePoolSize", "5"));
		int tMaxPoolSize = Integer.parseInt(aSettings.getProperty("job.executor.maxPoolSize", "30"));
		int tKeepAliveTime = Integer.parseInt(aSettings.getProperty("job.executor.keepAliveTimeMillis", "60000"));

		mJobs = new ConcurrentLinkedQueue<Job>();
		mExecutor = new ThreadPoolExecutor(tCorePoolSize, tMaxPoolSize, tKeepAliveTime, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
			private final AtomicInteger mThreadNumber;
			private final ThreadGroup mThreadGroup;

			{
				mThreadNumber = new AtomicInteger(1);
				java.lang.SecurityManager tSecurityManager = System.getSecurityManager();
				mThreadGroup = (tSecurityManager != null) ? tSecurityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
			}

			public Thread newThread(Runnable aRunnable) {
				Thread t = new Thread(mThreadGroup, aRunnable, "task-" + mThreadNumber.getAndIncrement(), 0);
				if (t.isDaemon()) {
					t.setDaemon(false);
				}
				if (t.getPriority() != Thread.NORM_PRIORITY) {
					t.setPriority(Thread.NORM_PRIORITY);
				}
				return t;
			}
		}) {
			@Override
			protected void beforeExecute(Thread aThread, Runnable aRunnable) {
				IRCDaemon.setCurrentConnectedEntity(aThread, ((Job) aRunnable).getOwner());
				super.beforeExecute(aThread, aRunnable);
			}

			@Override
			protected void afterExecute(Runnable aRunnable, Throwable aThrowable) {
				super.afterExecute(aRunnable, aThrowable);
				mJobs.remove(aRunnable);
				IRCDaemon.setCurrentConnectedEntity(null);
			}
		};
	}

	public void shutdown() {
		mExecutor.shutdownNow();
		mExecutor = null;
		mJobs.clear();
	}

	public String getName() {
		return JobManager.class.getSimpleName();
	}
}