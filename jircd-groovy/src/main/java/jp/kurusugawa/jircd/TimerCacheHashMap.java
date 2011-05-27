package jp.kurusugawa.jircd;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

public class TimerCacheHashMap<KetType, ValueType> extends ConcurrentHashMap<KetType, ValueType> {
	private static final long serialVersionUID = 4038652754186828042L;

	private static final Logger LOG = Logger.getLogger(TimerCacheHashMap.class);

	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private static final ThreadFactory THREAD_FACTORY;

	private static ScheduledExecutorService EXECUTOR;

	static {
		THREAD_FACTORY = Executors.defaultThreadFactory();
		EXECUTOR = Executors.newScheduledThreadPool(1, new ThreadFactory() {
			public Thread newThread(Runnable aRunnable) {
				Thread tNewThread = THREAD_FACTORY.newThread(aRunnable);
				tNewThread.setDaemon(true);
				return tNewThread;
			}

		});
	}

	public static void setExecutorService(ScheduledExecutorService aExecutorService) {
		EXECUTOR = aExecutorService;
	}

	public static ScheduledExecutorService getExecutorService() {
		return EXECUTOR;
	}

	private static final int DEFAULT_CACHE_CLEAR_PERIOD_MILLIS = 5000;
	private static final int DEFAULT_CACHE_CLEAR_PERIOD_RESCHEDULE_THRESHOLD_MILLIS = 4000;

	private Future<?> mFuture;
	private final ReentrantLock mLock;
	private final Runnable mTimerTask;
	private long mNextCacheClearTimeMillis;

	private int mCacheClearPeriodMillis;

	public void setCacheClearPeriodMillis(int aCacheClearPeriodMillis) {
		mCacheClearPeriodMillis = aCacheClearPeriodMillis;
	}

	private int mCacheClearPeriodRescheduleThresholdMillis;

	public void setCacheClearPeriodRescheduleThresholdMillis(int aCacheClearPeriodRescheduleThresholdMillis) {
		mCacheClearPeriodRescheduleThresholdMillis = aCacheClearPeriodRescheduleThresholdMillis;
	}

	public TimerCacheHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
	}

	public TimerCacheHashMap(int aInitialCapacity) {
		this(aInitialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public TimerCacheHashMap(Map<KetType, ValueType> aMap) {
		this(Math.max((int) (aMap.size() / DEFAULT_LOAD_FACTOR) + 1, DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
		putAll(aMap);
	}

	public TimerCacheHashMap(int aInitialCapacity, float aLoadFactor) {
		super(aInitialCapacity, aLoadFactor);
		mCacheClearPeriodMillis = DEFAULT_CACHE_CLEAR_PERIOD_MILLIS;
		mCacheClearPeriodRescheduleThresholdMillis = DEFAULT_CACHE_CLEAR_PERIOD_RESCHEDULE_THRESHOLD_MILLIS;
		mLock = new ReentrantLock();
		mTimerTask = new Runnable() {
			public void run() {
				mNextCacheClearTimeMillis = System.currentTimeMillis() + mCacheClearPeriodMillis;

				boolean tIsInfoEnabled = LOG.isInfoEnabled();
				try {
					if (tIsInfoEnabled) {
						LOG.info(System.identityHashCode(TimerCacheHashMap.this) + ".begin clear entry(=" + size() + ")");
					}

					clearCache();
					if (isEmpty()) {
						mFuture.cancel(false);
						mFuture = null;
					}

				} finally {
					if (tIsInfoEnabled) {
						LOG.info(System.identityHashCode(TimerCacheHashMap.this) + ".end clear. entry(=" + size() + ")");
					}
				}
			}
		};
	}

	public ReentrantLock getLock() {
		return mLock;
	}

	@SuppressWarnings("unchecked")
	protected void clearCache() {
		Map.Entry<KetType, ValueType>[] tEntries = entrySet().toArray(new Map.Entry[size()]);
		for (Map.Entry<KetType, ValueType> tEntry : tEntries) {
			try {
				if (freeIfPossible(tEntry)) {
					remove(tEntry.getKey());
				}
			} catch (Exception e) {
				LOG.warn(System.identityHashCode(this) + ".ignore exception", e);
			}
		}
	}

	@Override
	public void clear() {
		try {
			super.clear();
		} finally {
			calculateNextCacheClearTime();
		}
	}

	@Override
	public ValueType get(Object aKey) {
		try {
			return super.get(aKey);
		} finally {
			calculateNextCacheClearTime();
		}
	}

	@Override
	public ValueType put(KetType aKey, ValueType aValue) {
		try {
			return super.put(aKey, aValue);
		} finally {
			calculateNextCacheClearTime();
		}
	}

	@Override
	public void putAll(Map<? extends KetType, ? extends ValueType> aMap) {
		try {
			super.putAll(aMap);
		} finally {
			calculateNextCacheClearTime();
		}
	}

	@Override
	public boolean containsKey(Object aKey) {
		try {
			return super.containsKey(aKey);
		} finally {
			calculateNextCacheClearTime();
		}
	}

	@Override
	public boolean containsValue(Object aValue) {
		try {
			return super.containsValue(aValue);
		} finally {
			calculateNextCacheClearTime();
		}
	}

	protected void calculateNextCacheClearTime() {
		long tCurrentTimeMillis = System.currentTimeMillis();
		if (mFuture != null) {
			if (mNextCacheClearTimeMillis - tCurrentTimeMillis > mCacheClearPeriodRescheduleThresholdMillis) {
				// 次回キャッシュクリアまでにCACHE_CLEAR_PERIOD_RESCHEDULE_THRESHOLD_MILLISある場合は何もしない
				return;
			}
			mFuture.cancel(false);
		}
		int tCacheClearPeriodMillis = mCacheClearPeriodMillis;
		mNextCacheClearTimeMillis = tCurrentTimeMillis + tCacheClearPeriodMillis;
		mFuture = EXECUTOR.scheduleWithFixedDelay(mTimerTask, tCacheClearPeriodMillis, tCacheClearPeriodMillis, TimeUnit.MILLISECONDS);
		if (LOG.isInfoEnabled()) {
			LOG.info(System.identityHashCode(this) + ".reschedule: " + mNextCacheClearTimeMillis);
		}
	}

	protected boolean freeIfPossible(Map.Entry<KetType, ValueType> aElement) {
		return true;
	}
}