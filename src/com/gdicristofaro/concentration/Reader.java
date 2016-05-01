package com.gdicristofaro.concentration;

import java.io.IOException;

// used to read from file, string path, url
@FunctionalInterface
public interface Reader<I, O> {
	O convert(I input) throws IOException;
}
