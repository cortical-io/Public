package io.cortical.iris.message;

import rx.Observable;
import rx.Observable.Transformer;
import rx.functions.FuncN;

/**
 * Used in conjunction with the {@link MergeOrPassthruValve} to compose an
 * output type considered to be the merge type of the valve, and supply those
 * functions with the meta information they need to compose the functions. 
 * In addition, a {@link Transformer} can be used to provide a transform to the pass through
 * functionality when merging of the valve is disabled.
 * 
 * @author cogmission
 *
 * @param <T>   the type of the input
 * @param <R>   the return type
 * @param <P>   the "pass through" type (most often same as input since it is a pass through,
 *              but can be transformed to produce another type when passing through.
 */
@SuppressWarnings("unchecked")
public class MergeSupplier<T, R, P> {   
    private Class<T> c1;
    private Class<R> c2;
    private Class<P> c3;
    private FuncN<R> f;
    private Transformer<T, P> passthruTransform;
    private int numInputs;
    
    /**
     * Creates a new {@code MergeSupplier} which uses the default
     * pass through transformer which is an identity transformer (same out as
     * in).
     * 
     * @param c1            the class type of the input
     * @param c2            the class type of the return value
     * @param c3            the class type of the pass through (same as input if no
     *                      since no transform is specified in this constructor) (uses
     *                      identity)
     * @param numInputs     the number of inputs
     * @param f             the {@link FuncN} used as the merged type supplier
     */
    public MergeSupplier(Class<T> c1, Class<R> c2, Class<P> c3, int numInputs, FuncN<R> f) {
        this(c1, c2, c3, numInputs, f, t -> (Observable<P>)t);
    }
    
    /**
     * Creates a new {@code MergeSupplier} which uses the {@link Transformer} specified to
     * create the emitted merge types.
     * 
     * @param c1                    the class type of the input
     * @param c2                    the class type of the return value
     * @param c3                    the class type of the pass through
     * @param numInputs             the number of inputs
     * @param f                     the {@link FuncN} used as the merged type supplier
     * @param passthruTransform     the Transformer applied to the pass through to provide a different
     *                              type to be passed through to the output.
     */
    public MergeSupplier(Class<T> c1, Class<R> c2, Class<P> c3, int numInputs, FuncN<R> f,
        Transformer<T, P> passthruTransform) {
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.f = f;
        this.numInputs = numInputs;
        this.passthruTransform = passthruTransform;
    }
    
    public Class<T> getInputType() { return c1; }
    public Class<R> getReturnType() { return c2; }
    public Class<P> getPassthruReturnType() { return c3; }
    public Transformer<T, P> getPassthruTransform() { return passthruTransform; }
    public FuncN<R> getSupplyFunc() { return f; }
    public int getNumInputs() { return numInputs; }
}