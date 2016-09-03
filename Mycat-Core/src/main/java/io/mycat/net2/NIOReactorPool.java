package io.mycat.net2;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.mycat.SQLEngineCtx;

public class NIOReactorPool {
	private final NIOReactor[] reactors;
	private volatile int nextReactor;

	public NIOReactorPool(String name, int poolSize, SharedBufferPool shearedBufferPool) throws IOException {
		reactors = new NIOReactor[poolSize];
		Map<String,NIOReactor> reactorMap=new HashMap<String,NIOReactor>();
		for (int i = 0; i < poolSize; i++) {
			NIOReactor reactor = new NIOReactor(name + "-" + i, shearedBufferPool);
			reactors[i] = reactor;
			reactor.startup();
			reactorMap.put(reactor.getName(), reactor);
			 
		}
		
	}

	public NIOReactor[] getAllReactors()
	{
		return reactors;
	}
	public NIOReactor getSpecialActor(String name)
	{
		for(NIOReactor reactor:reactors)
		{
			if(reactor.getName().equals(name)) 
			{
				return reactor;
			}
		}
		return null;
	}
	public NIOReactor getNextReactor() {
		int i = ++nextReactor;
		if (i >= reactors.length) {
			i = nextReactor = 0;
		}
		return reactors[i];
	}
}
