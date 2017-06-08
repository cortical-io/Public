package io.cortical.iris.message;

import java.util.Arrays;

import io.cortical.iris.ui.custom.property.OccurrenceProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Subscription;
import rx.functions.FuncN;
import rx.subjects.PublishSubject;

/**
 * This class operates in two modes; the default is where the "{@link Observable#combineLatest(java.util.List, FuncN)}
 * function is used to provide "merged" output (using a function the user specifies to create the "merged" output type).
 * 
 * For example, there are two inputs, a first and a second. The user's program calls {@link #addPrimaryInput(Object)}, and
 * nothing happens. The user's program then calls {@link #addSecondaryInput(Object)}; at that time the function specified by
 * the user is called to create and emit the "merged" output. After this, a call to either addFirst... or addSecond, will
 * result in a new merged output being emitted to the merged output observer.
 * 
 * When {@link #mergeEnabled()} returns false (by a call to {@link #disableMerge()}, any call to {@link #addPrimaryInput(Object)}
 * or {@link #addSecondaryInput(Object)} will result in an output emitted either to the first or second output observer (depending
 * on which was called). The emitted item in most cases would be the input, but in the case where the user specifies a {@link Transformer}
 * to be used to create a different type, that transformer will be used and the type emitted by the transformer will be emitted
 * to the first or second observer.
 * 
 * <pre>
 * 
 * addPrimaryInput()             addSecondaryInput()
 *             \                   /
 *               \               /
 *                 \     ^     /
 *                   \ /   \ /
 *                    / IN  \
 *                   /_______\
 *                   \       /
 *                    \ OUT /
 *                    /\   /\
 *                  /    v    \
 *                 /     |     \
 *                /      |      \
 *    Out from first     |   Out from second      <---- After call to "disableObserver()"
 *                       |
 *                       |                                     --or--  (mutually exclusive)
 *                       |
 *                 Merged Output                  <---- Default & after "enableObserver()"
 *                 
 * </pre>
 * 
 * This class uses the RxJava Observable function {@link Observable#combineLatest(java.util.List, FuncN)}
 * to manage the logic when {@link #mergeEnabled()} returns true. (see below)
 * <p>
 * <img src="./doc-files/combineLatest.png" />
 * </p>
 * 
 * The user may specify the function used to create the "merged output" by submitting a {@link MergeSupplier} which
 * contains the {@link FuncN} and meta information used to create the innards and glue used internally to create
 * the {@link Observable}s used to produce the various outputs.
 * 
 * @author cogmission
 *
 * @param <T>   the type of the input
 * @param <R>   the return type
 * @param <P>   the "pass through" type (most often same as input since it is a pass through,
 *              but can be transformed to produce another type when passing through.
 */
public class MergeOrPassthruValve<T, R, P> {
    PublishSubject<T> in1 = PublishSubject.create();
    PublishSubject<T> in2 = PublishSubject.create();
    
    T lastPrimary;
    T lastSecondary;
    
    Subscription subscription1;
    Subscription subscription2;
    Subscription mergeSubscription;
    Subscription primarySubscription;
    Subscription secondarySubscription;
    
    Observer<R> mergeObserver;
    Observer<P> primaryObserver;
    Observer<P> secondaryObserver;
    
    Observable<R> pipe;
    
    MergeSupplier<T, R, P> supplier;
    
    boolean mergeEnabled = true;
    boolean wasMergeDisabled;
    
    BooleanProperty mergeEnabledProperty = new SimpleBooleanProperty(true);
    OccurrenceProperty inputSwapProperty = new OccurrenceProperty();
    ObjectProperty<T> primaryInputProperty = new SimpleObjectProperty<T>();
    ObjectProperty<T> secondaryInputProperty = new SimpleObjectProperty<T>();
    
    private OccurrenceProperty primaryInputInternalProperty = new OccurrenceProperty();
    private OccurrenceProperty secondaryInputInternalProperty = new OccurrenceProperty();
    
    
    
    /**
     * Creates a new {@code MergeOrPassthruValve} which uses the default
     * pass through transformer which is an identity transformer (same out as
     * in).
     * 
     * @param supplier  the supplier used to create the emitted merge types from 
     *                  the inputs supplied previously.
     */
    public MergeOrPassthruValve(MergeSupplier<T, R, P> supplier) {
        setSupplier((MergeSupplier<T, R, P>)supplier);
        
        mergeEnabledProperty.addListener((v,o,n) ->   { if(n) enableMerge(); else disableMerge(); } );
        inputSwapProperty.addListener((v,o,n) -> swap());
        primaryInputProperty.addListener((v,o,n) -> {
            if(n == null) return;
            lastPrimary = n;
            primaryInputInternalProperty.set();
            // Clear the last state so this will propagate the same value twice
            primaryInputProperty.set(null); 
        });
        secondaryInputProperty.addListener((v,o,n) -> {
            if(n == null) return;
            lastSecondary = n;
            secondaryInputInternalProperty.set();
            // Clear the last state so this will propagate the same value twice
            secondaryInputProperty.set(null); 
        });
        primaryInputInternalProperty.addListener((v,o,n) -> addPrimaryInput(lastPrimary));
        secondaryInputInternalProperty.addListener((v,o,n) -> addSecondaryInput(lastSecondary));
    }
    
    /**
     * Returns the property that reflects the merge enabled state.
     * @return
     */
    public BooleanProperty mergeEnabledProperty() {
        return mergeEnabledProperty;
    }
    
    /**
     * Returns the property that reflects "swap" requests,
     * meaning to switch the primary and secondary inputs.
     * @return
     */
    public OccurrenceProperty inputSwapProperty() {
        return inputSwapProperty;
    }
    
    /**
     * Returns the property that reflects the submission of an input
     * to the "Primary" side.
     * @return
     */
    public ObjectProperty<T> primaryInputProperty() {
        return primaryInputProperty;
    }
    
    /**
     * Returns the property that reflects the submission of an input
     * to the "Secondary" side.
     * @return
     */
    public ObjectProperty<T> secondaryInputProperty() {
        return secondaryInputProperty;
    }
    
    /**
     * Sets a new {@link MergeSupplier} on this valve
     * @param supplier
     */
    @SuppressWarnings("unchecked")
    public void setSupplier(MergeSupplier<T, R, P> supplier) {
        this.supplier = supplier;
        
        subscription1 = in1.subscribe(s -> {});
        subscription2 = in2.subscribe(s -> {});
        
        pipe = (Observable<R>)Observable.combineLatest(
            Arrays.<Observable<R>>asList(new Observable[] {in1, in2}), supplier.getSupplyFunc());
    }
    
    /**
     * Not allowed when merge is enabled; this call makes the
     * previous secondary observer, the primary observer.
     */
    @SuppressWarnings("unchecked")
    public void shiftPrimary() {
        if(mergeEnabled) return;
        
        primarySubscription.unsubscribe();
        
        PublishSubject<T> hold = in1;
        in1 = in2;
        in2 = hold;
        
        pipe = (Observable<R>)Observable.combineLatest(
            Arrays.<Observable<R>>asList(new Observable[] {in1, in2}), supplier.getSupplyFunc());
        
        connectPrimary();
        
        addPrimaryInput(lastSecondary);
    }
    
    /**
     * Called to swap the primary and secondary inputs.
     */
    @SuppressWarnings("unchecked")
    public void swap() {
        // Can't swap if pipeline doesn't connect the two inputs
        if(!mergeEnabled) return;
        
        PublishSubject<T> hold = in1;
        in1 = in2;
        in2 = hold;
        
        pipe = (Observable<R>)Observable.combineLatest(
            Arrays.<Observable<R>>asList(new Observable[] {in1, in2}), supplier.getSupplyFunc());
        
        setMergeObserver(mergeObserver);
        
        T savedPrimary = lastPrimary;
        addPrimaryInput(lastSecondary);
        addSecondaryInput(savedPrimary);
    }
    
    public T getLastPrimaryInput() {
        return lastPrimary;
    }
    
    public T getLastSecondaryInput() {
        return lastSecondary;
    }
    
    /**
     * Stops the individual (pass through) process emissions and enables
     * the merging emissions instead.
     */
    public void enableMerge() {
        if(mergeEnabled) return;
        
        wasMergeDisabled = true;
        mergeEnabled = true;
        
        if(primarySubscription != null) {
            primarySubscription.unsubscribe();
        }
        if(secondarySubscription != null) {
            secondarySubscription.unsubscribe();
        }
        
        setMergeObserver(mergeObserver);
    }
    
    /**
     * Stops the merge {@link Observable} from emitting new 
     * merge outputs and enables the pass through of the individual
     * input processing to their respective outputs instead.
     */
    public void disableMerge() {
        if(!mergeEnabled) return;
        
        if(primaryObserver != null) {
            connectPrimary();
        }
        if(secondaryObserver != null) {
            connectSecondary();
        }
        
        mergeSubscription.unsubscribe();
        
        mergeEnabled = false;
    }
    
    /**
     * Returns a flag indicating whether the merging is enabled
     * or disabled.
     * 
     * @return  true if enabled, false if not.
     */
    public boolean mergeEnabled() {
        return mergeEnabled;
    }
    
    public T lastPrimary() {
        return lastPrimary;
    }
    
    public T lastSecondary() {
        return lastSecondary;
    }
    
    /**
     * Sets the {@link Observer} called when merging is disabled and there
     * is a new primary input.
     * @param r the Observer to be called
     */
    public void setPrimaryObserver(Observer<P> r) {
        primaryObserver = r;
    }
    
    /**
     * Sets the {@link Observer} called when merging is disabled and there
     * is a new secondary input.
     * @param r the Observer to be called
     */
    public void setSecondaryObserver(Observer<P> r) {
        secondaryObserver = r;
    }
    
    /**
     * Sets the {@link Observer} called when merging is enabled and there is
     * either a new primary or new secondary input and a previous primary or
     * secondary input has been added such that one of each now exists.
     * 
     * @param r the Observer to be called
     */
    public void setMergeObserver(Observer<R> o) {
        if(mergeSubscription != null) {
            mergeSubscription.unsubscribe();
        }
        mergeObserver = o;
        mergeSubscription = pipe.subscribe(o);
    }
    
    /**
     * Returns the {@link PublishSubject} used to pipe input to the 
     * primary side. A PublishSubject can act as both an Observable which 
     * can be subscribed to by an Observer, and an Subject which can be 
     * used to submit input by calling {@link PublishSubject#onNext(Object)} 
     *  
     * @return  the PublishSubject which is the primary input Observable
     */
    public PublishSubject<T> getPrimaryInput() {
        return in1;
    }
    
    /**
     * Returns the {@link PublishSubject} used to pipe input to the 
     * primary side. A PublishSubject can act as both an Observable which 
     * can be subscribed to by an Observer, and an Subject which can be 
     * used to submit input by calling {@link PublishSubject#onNext(Object)} 
     *  
     * @return  the PublishSubject which is the secondary input Observable
     */
    public PublishSubject<T> getSecondaryInput() {
        return in2;
    }
    
    /**
     * Called to add an input to the primary side. If the secondary side has
     * had any submissions, then this addition will cause the merge function
     * to be executed and a merge result to be emitted via the merge observer.
     * 
     * @param r the object inputted
     */
    public void addPrimaryInput(T r) {
        if(wasMergeDisabled) {
            wasMergeDisabled = false;
            if(lastSecondary != null) {
                addSecondaryInput(lastSecondary);
            }
        }
        lastPrimary = r;
        in1.onNext(r);
    }
    
    /**
     * Called to add an input to the secondary side. If the primary side has
     * had any submissions, then this addition will cause the merge function
     * to be executed and a merge result to be emitted via the merge observer.
     * 
     * @param r the object inputted
     */
    public void addSecondaryInput(T r) {
        if(wasMergeDisabled) {
            wasMergeDisabled = false;
            if(lastPrimary != null) {
                addPrimaryInput(lastPrimary);
            }
        }
        lastSecondary = r;
        in2.onNext(r);
    }
    
    /*
     * Called to connect or reconnect after disabling
     */
    private void connectPrimary() {
        Observable<P> p = in1
            .ofType(supplier.getInputType())
            .compose(t -> (Observable<P>)supplier.getPassthruTransform().call(t));
        
        primarySubscription = p.subscribe(primaryObserver);
    }
    
    /*
     * Called to connect or reconnect after disabling
     */
    private void connectSecondary() {
        Observable<P> p = in2
            .ofType(supplier.getInputType())
            .compose(t -> (Observable<P>)supplier.getPassthruTransform().call(t));
        
        secondarySubscription = p.subscribe(secondaryObserver);
    }

}