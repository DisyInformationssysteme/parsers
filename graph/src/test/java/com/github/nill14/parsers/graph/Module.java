package com.github.nill14.parsers.graph;

import com.github.nill14.parsers.dependency.IDependencyDescriptor;
import com.github.nill14.parsers.dependency.IDependencyDescriptorBuilder;
import com.github.nill14.parsers.dependency.impl.DependencyDescriptor;
import com.google.common.base.Function;

public class Module {
	
	private final IDependencyDescriptor<String> collector;
	
	private Module(Builder builder) {
		collector = builder.builder.build();
	}

	public IDependencyDescriptor<String> getCollector() {
		return collector;
	}
	
	public static Builder builder(String name) {
		return new Builder(name);
	}
	
	@Override
	public String toString() {
		return collector.toString();
	}
	
	public static final Function<Module, IDependencyDescriptor<String>> adapterFunction = new Function<Module, IDependencyDescriptor<String>>() {
		
		@Override
		public IDependencyDescriptor<String> apply(Module input) {
			return input.getCollector();
		}
	};
	
	public static class Builder implements IDependencyDescriptorBuilder<String> {
		
		private final IDependencyDescriptorBuilder<String> builder;
		
		public Builder(String name) {
			builder = DependencyDescriptor.builder(name);
		}
		
		@Override
		public Builder uses(String fqn) {
			builder.uses(fqn);
			return this;
		}

		@Override
		public Builder usesOptionally(String fqn) {
			builder.usesOptionally(fqn);
			return this;
		}
		
		@Override
		public Builder provides(String fqn) {
			builder.provides(fqn);
			return this;
		}		

		@Override
		public IDependencyDescriptorBuilder<String> executionPriority(int priority) {
			builder.executionPriority(priority);
			return this;
		}
		
		@Override
		public IDependencyDescriptor<String> build() {
			return builder.build();
		}

		public Module buildModule() {
			return new Module(this);
		}
		
	}

}
