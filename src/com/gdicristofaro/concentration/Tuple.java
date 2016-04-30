package com.gdicristofaro.concentration;

public class Tuple<F,S> {
	public static <F,S> Tuple<F,S> get(F first, S second) {
		return new Tuple<F,S>(first,second);
	}
	
	private final F first;
	private final S second;
	
	public Tuple(F first, S second) {
		this.first = first;
		this.second = second;
	}
	
	public F getFirst() {
		return first;
	}
	
	public S getSecond() {
		return second;
	}
}
