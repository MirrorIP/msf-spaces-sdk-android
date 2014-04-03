package de.imc.mirror.sdk.android;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Helper class to provide blocking IQ requests. 
 * A latch is set up which is released when a result is available.
 * @author simon.schwantzer(at)im-c.de
 *
 */
public class RequestFuture<E> implements Future<E> {
	private volatile E response = null;
    private volatile boolean cancelled = false;
    private final CountDownLatch countDownLatch;
    
	public RequestFuture() {
		countDownLatch = new CountDownLatch(1);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (isDone()) {
			return false;
		} else {
			countDownLatch.countDown();
			cancelled = true;
			return !isDone();
		}
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isDone() {
		return countDownLatch.getCount() == 0;
	}

	@Override
	public E get() throws InterruptedException, ExecutionException {
		countDownLatch.await();
		return response;
	}

	@Override
	public E get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!countDownLatch.await(timeout, unit)) {
			// timed out
			throw new TimeoutException();
		}
		
		return response;
	}
	
	/**
	 * Sets the response for the request.
	 * When called, the countdown latch is released and the get()-calls are no longer blocked.
	 * This method is called when the related IQ response is received by the SpacesService.
	 * @param response IQ response send by the server.
	 */
	public void setResponse(E response) {
		this.response = response;
		countDownLatch.countDown();
	}
	
}