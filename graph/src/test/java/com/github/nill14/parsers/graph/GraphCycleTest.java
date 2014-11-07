package com.github.nill14.parsers.graph;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Deque;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.nill14.parsers.dependency.UnsatisfiedDependencyException;
import com.github.nill14.parsers.dependency.IDependencyGraphFactory;
import com.github.nill14.parsers.dependency.impl.DependencyGraphBuilder;
import com.github.nill14.parsers.graph.DirectedGraph;
import com.github.nill14.parsers.graph.GraphEdge;
import com.github.nill14.parsers.graph.utils.GraphCycleDetector;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class GraphCycleTest {
	
	private static final Logger log = LoggerFactory.getLogger(GraphCycleTest.class);
	
	private DirectedGraph<Module, GraphEdge<Module>> graph;
	private Set<Module> modules;
	private IDependencyGraphFactory<Module> dependencyBuilder;
	private ImmutableMap<String, Module> moduleIndex;



	@Before
	public void init() throws UnsatisfiedDependencyException {
		modules = ImmutableSet.of(
			Module.builder("A")
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
				
			//cycle	
			Module.builder("E")
				.dependsOnOptionally("G")
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
				.dependsOnOptionally("C")
				.buildModule(),
			Module.builder("J")
				.provides("A")
				.buildModule(),
				
			// another cycle	
			Module.builder("K")
				.dependsOn("L")
				.buildModule(),
			Module.builder("L")
				.dependsOn("K")
				.buildModule(),
				
			Module.builder("M")
				.dependsOn("H")
				.buildModule()
		);		

		dependencyBuilder = DependencyGraphBuilder.newInstance(modules, Module.adapterFunction);
		graph = dependencyBuilder.getDirectedGraph();
		
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
	
	@Test
	public void testBuild() {
		assertEquals(13, graph.nodes().size());
		assertDependency("A", "B");
		assertDependency("A", "C");
		assertDependency("B", "C");
		assertDependency("M", "A");
		assertDependency("H", "M");
	}
	
	@Test
	public void testCycles() {
		Collection<Deque<Module>> cycles = new GraphCycleDetector<>(graph).getNontrivialCycles();
		
		
		log.info("{}", cycles);
		assertEquals(3, cycles.size());
	}

}
