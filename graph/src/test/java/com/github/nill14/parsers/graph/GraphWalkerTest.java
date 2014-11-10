package com.github.nill14.parsers.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nill14.parsers.dependency.IDependencyGraph;
import com.github.nill14.parsers.dependency.ModuleConsumer;
import com.github.nill14.parsers.dependency.UnsatisfiedDependencyException;
import com.github.nill14.parsers.dependency.impl.DependencyGraphFactory;
import com.github.nill14.parsers.dependency.impl.DependencyTree;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class GraphWalkerTest {
	
	private static final Logger log = LoggerFactory.getLogger(GraphWalkerTest.class);
	private final ExecutorService executor = Executors.newFixedThreadPool(8);
	private DirectedGraph<Module, GraphEdge<Module>> graph;
	private Set<Module> modules;
	private IDependencyGraph<Module> dependencyGraph;
	private ImmutableMap<String, Module> moduleIndex;


	@Rule public ExpectedException thrown = ExpectedException.none();

	@Before
	public void init() throws CyclicGraphException, UnsatisfiedDependencyException {
		modules = ImmutableSet.of(
			Module.builder("A")
				.provides("A")
				.dependsOn("M")
				.buildModule(),
			Module.builder("B")
				.dependsOn("A")
				.buildModule(),
			Module.builder("C")
				.dependsOn("A")
				.dependsOn("B")
				.buildModule(),	
				
			//not connected	
			Module.builder("D")
				.buildModule(),
				
			Module.builder("E")
				.buildModule(),	
			Module.builder("F")
				.dependsOn("E")
				.buildModule(),
			Module.builder("G")
				.dependsOn("F")
				.buildModule(),
				
			Module.builder("H")
				.dependsOn("C")
				.buildModule(),
			Module.builder("I")
				.dependsOn("C")
				.buildModule(),
			Module.builder("J")
				.provides("A")
				.buildModule(),
				
			Module.builder("K")
				.buildModule(),
			Module.builder("L")
				.dependsOn("K")
				.buildModule(),
				
			Module.builder("M")
				.buildModule()
		);		
		
		dependencyGraph = DependencyGraphFactory.newInstance(modules, Module.adapterFunction);
		graph = dependencyGraph.getGraph();
		
		moduleIndex = Maps.uniqueIndex(modules, new Function<Module, String>() {

			@Override
			public String apply(Module input) {
				return input.toString();
			}
		});
	}
	
	public Module findModule(String fqn) {
		return moduleIndex.get(fqn);
	}

	public void assertDependency(String a, String b) {
		Module nodeA = findModule(a);
		Module nodeB = findModule(b);
		
		assertTrue(nodeA + "->" + nodeB, graph.successors(nodeA).contains(nodeB));
		assertTrue(nodeA + "->" + nodeB, graph.predecessors(nodeB).contains(nodeA));
	}

	private void assertTopoOrder(List<Module> topologicalOrdering) {
		log.info("{}", topologicalOrdering);
		assertEquals(modules.size(), topologicalOrdering.size());
		for (int i = 0; i < topologicalOrdering.size() - 1; i++) {
			Module n = topologicalOrdering.get(i);
			for (int j = i; j < topologicalOrdering.size(); j++) {
				Module m = topologicalOrdering.get(j);
				
				assertFalse(n + "<-" + m, graph.predecessors(n).contains(m));
			}
		}
	}
	
	@Test
	public void testWalk() throws InterruptedException, ExecutionException {
		final AtomicInteger count = new AtomicInteger();
		final Queue<Module> executionOrder = new ConcurrentLinkedQueue<>();
		
		dependencyGraph.walkGraph(executor, new ModuleConsumer<Module>() {
			
			@Override
			public void process(Module module) throws Exception {
				log.info("Starting module {}", module);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				log.info("Completing module {}", module);
				count.incrementAndGet();
				executionOrder.add(module);
			}
		});
		
		assertEquals(modules.size(), count.get());
		assertTopoOrder(Lists.newArrayList(executionOrder));
	}
	
	@Test
	public void testWalkSynchronously() throws InterruptedException, ExecutionException {
		final AtomicInteger count = new AtomicInteger();
		final Queue<Module> executionOrder = new ConcurrentLinkedQueue<>();
		
		dependencyGraph.iterateTopoOrder(new ModuleConsumer<Module>() {
			
			@Override
			public void process(Module module) throws Exception {
				log.info("Starting module {}", module);
				log.info("Completing module {}", module);
				count.incrementAndGet();
				executionOrder.add(module);
			}
		});
		
		assertEquals(modules.size(), count.get());
		assertEquals(dependencyGraph.getTopologicalOrder(), Lists.newArrayList(executionOrder));
	}	
	
	
	@Test
	public void testException() throws InterruptedException, IOException {
		final AtomicInteger count = new AtomicInteger();
		
		thrown.expect(IOException.class);
		thrown.expectMessage("test checked exception");
		
		try {
			dependencyGraph.walkGraph(executor, new ModuleConsumer<Module>() {
				
				@Override
				public void process(Module module) throws Exception {
					log.info("Starting module {}", module);
					Thread.sleep(100);
					log.info("Completing module {}", module);
					if (count.incrementAndGet() == 5) {
						throw new IOException("test checked exception");
					};
					
				}
			});
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			} else {
				throw new RuntimeException("Unexpected", e);
			}
		}
		
		assertEquals(modules.size(), count.get());
	}	
	
	@Test
	public void testLog() {
		Collection<String> lines = new DependencyTree<>(dependencyGraph, true).getLines();
		for (String line : lines) {
			log.info(line);
		}
	}

	@Test
	public void testExhaustException() throws InterruptedException, IOException {
		final AtomicInteger count = new AtomicInteger();
		
		thrown.expect(IOException.class);
		thrown.expectMessage("test checked exception");
		
		try {
			dependencyGraph.walkGraph(executor, new ModuleConsumer<Module>() {
				
				@Override
				public void process(Module module) throws Exception {
					log.info("Starting module {}", module);
					Thread.sleep(1);
					log.info("Completing module {}", module);
					if (count.incrementAndGet() == 1) {
						//wait until we park with lockCondition
						Thread.sleep(200);
						throw new IOException("test checked exception");
					};
					
				}
			});
		} catch (ExecutionException e) {
			if (e.getCause() instanceof IOException) {
				throw (IOException) e.getCause();
			} else {
				throw new RuntimeException("Unexpected", e);
			}
		}
		
		assertEquals(modules.size(), count.get());
	}
	
	
}
