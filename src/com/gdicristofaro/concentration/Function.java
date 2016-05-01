package com.gdicristofaro.concentration;

// convert from one type to another
@FunctionalInterface
public interface Function<I, O> {
	O convert(I input);
}
