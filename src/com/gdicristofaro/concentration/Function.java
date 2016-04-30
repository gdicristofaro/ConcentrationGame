package com.gdicristofaro.concentration;

// converts one item to another 

@FunctionalInterface
public interface Function<I, O> {
	O convert(I input);
}
