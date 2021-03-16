package it.unipr.ds.A1;

/**
 * Class that models a simple pair of values, with generic types
 *
 * @param <T1> Type of the first element
 * @param <T2> Type of the second element
 */
public class Pair<T1, T2> {
	private T1 first;
	private T2 second;
	
	public Pair(T1 first, T2 second) {
		this.first = first;
		this.second= second;
	}
	
	public T1 getFirst() {return this.first;}
	public T2 getSecond() {return this.second;}
}
