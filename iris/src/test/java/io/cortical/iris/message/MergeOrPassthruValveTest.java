package io.cortical.iris.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import io.cortical.iris.message.CompareResponse;
import io.cortical.iris.message.MergeOrPassthruValve;
import io.cortical.iris.message.MergeSupplier;
import io.cortical.iris.message.ServerRequest;
import io.cortical.iris.message.ServerResponse;
import io.cortical.retina.core.PosType;
import io.cortical.retina.model.Term;
import rx.observers.TestObserver;


public class MergeOrPassthruValveTest {
    
    @Test
    public void testRxValveFunctionality() {
        TestObserver<CompareResponse> testResponses;
        TestObserver<ServerResponse> primaryResponses;
        TestObserver<ServerResponse> secondaryResponses;
        
        MergeSupplier<ServerResponse, CompareResponse, ServerResponse> ms = new MergeSupplier<ServerResponse, CompareResponse, ServerResponse>(
            ServerResponse.class, CompareResponse.class, ServerResponse.class, 2, 
                (Object[] oa) -> new CompareResponse(((ServerResponse)oa[0]).getRequest(), ((ServerResponse)oa[1]).getRequest(), null, null, null, null));
        
        MergeOrPassthruValve<ServerResponse, CompareResponse, ServerResponse> valve = new MergeOrPassthruValve<>(ms);
        
        valve.setMergeObserver(testResponses = new TestObserver<CompareResponse>() {
            public void onNext(CompareResponse r) {
                super.onNext(r);
                try {
                    System.out.println("SINK: \n\t" + r.getPrimaryRequest().getModel().toJson() +
                        "\n\t" + r.getSecondaryRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        valve.setPrimaryObserver(primaryResponses = new TestObserver<ServerResponse>() {
            public void onNext(ServerResponse r) {
                super.onNext(r);
                try {
                    System.out.println("PRIMARY: \n\t" + r.getRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        valve.setSecondaryObserver(secondaryResponses = new TestObserver<ServerResponse>() {
            public void onNext(ServerResponse r) {
                super.onNext(r);
                try {
                    System.out.println("SECONDARY: \n\t" + r.getRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        
        ServerRequest sreq1 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("car"), 0, null);
        ServerRequest sreq2 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("truck"), 0, null);
        ServerResponse sr1 = new ServerResponse(sreq1, null, null, null, null, null, null, null, null, null);
        ServerResponse sr2 = new ServerResponse(sreq2, null, null, null, null, null, null, null, null, null);
        
        // First test
        System.out.println("\nFirst Test");
        valve.addPrimaryInput(sr1);
        assertEquals(0, testResponses.getOnNextEvents().size());
        valve.addSecondaryInput(sr2);
        assertEquals(1, testResponses.getOnNextEvents().size());
        
        // Second Test
        System.out.println("\nSecond Test");
        sreq2.setModel(new Term("pizza"));
        valve.addSecondaryInput(sr2);
        assertEquals(2, testResponses.getOnNextEvents().size());
        
        // Third test
        System.out.println("\nThird Test");
        sreq1.setModel(new Term("airplane"));
        valve.addPrimaryInput(sr1);
        assertEquals(3, testResponses.getOnNextEvents().size());
        
        // Test that swap() will emit another combined input with 1 & 2 swapped
        System.out.println("\nSwap Test");
        valve.swap();
        assertEquals(4, testResponses.getOnNextEvents().size());
        assertEquals("pizza", ((Term)valve.lastPrimary().getRequest().getModel()).getTerm());
        assertEquals("airplane", ((Term)valve.lastSecondary().getRequest().getModel()).getTerm());
        
        //////////
        
        // Fifth test
        System.out.println("\nFifth Test");
        valve.addPrimaryInput(sr1);
        assertEquals(0, primaryResponses.getOnNextEvents().size());
        assertEquals(0, secondaryResponses.getOnNextEvents().size());
        assertEquals(5, testResponses.getOnNextEvents().size());
        
        // Sixth test
        System.out.println("\nSixth Test");
        valve.addSecondaryInput(sr2);
        assertEquals(0, primaryResponses.getOnNextEvents().size());
        assertEquals(0, secondaryResponses.getOnNextEvents().size());
        assertEquals(6, testResponses.getOnNextEvents().size());
        
        // Seventh test
        System.out.println("\nSeventh Test");
        valve.disableMerge();
        valve.addPrimaryInput(sr1);
        assertEquals(1, primaryResponses.getOnNextEvents().size());
        assertEquals(0, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(6, testResponses.getOnNextEvents().size()); 
        
        // Eighth test
        System.out.println("\nEighth Test");
        valve.addSecondaryInput(sr2);
        assertEquals(1, primaryResponses.getOnNextEvents().size());
        assertEquals(1, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(6, testResponses.getOnNextEvents().size());
        
        // Test swap (The Pipeline is disabled so no changes should occur to outputs)
        System.out.println("\nDisabled Swap Test");
        valve.swap();
        assertEquals(1, primaryResponses.getOnNextEvents().size());
        assertEquals(1, secondaryResponses.getOnNextEvents().size());
        assertEquals(6, testResponses.getOnNextEvents().size());
        assertEquals("airplane", ((Term)valve.lastPrimary().getRequest().getModel()).getTerm());
        assertEquals("pizza", ((Term)valve.lastSecondary().getRequest().getModel()).getTerm());
        
        // Add a few more responses while disabled
        System.out.println("\nMore disabled Tests");
        valve.addPrimaryInput(sr1);
        valve.addPrimaryInput(sr1);
        valve.addSecondaryInput(sr2);
        assertEquals(3, primaryResponses.getOnNextEvents().size());
        assertEquals(2, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(6, testResponses.getOnNextEvents().size());
        
        // Re-enable Pipeline test 1
        System.out.println("\n Re-enable pipeline 1");
        valve.enableMerge();
        valve.addSecondaryInput(sr2);
        assertEquals(3, primaryResponses.getOnNextEvents().size());
        assertEquals(2, secondaryResponses.getOnNextEvents().size());
        assertEquals(7, testResponses.getOnNextEvents().size());
        
        // Re-enable Pipeline test 2
        System.out.println("\n Re-enable pipeline 2");
        valve.addPrimaryInput(sr1);
        assertEquals(3, primaryResponses.getOnNextEvents().size());
        assertEquals(2, secondaryResponses.getOnNextEvents().size());
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        // Try disabling again
        System.out.println("\n 2nd Disable test");
        valve.disableMerge();
        valve.addPrimaryInput(sr1);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(2, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        // Disabled 2nd input again
        System.out.println("\n Disabled 2nd input again");
        valve.addSecondaryInput(sr2);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(3, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        // Disabled 2nd input again
        System.out.println("\n Disabled 2nd input another time");
        // Make sure calling disable a second time has no effect
        valve.disableMerge(); 
        valve.addSecondaryInput(sr2);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        // Call enable after 2nd disable - ensure no additions and no changes
        System.out.println("\n Call enable after 2nd disable");
        valve.enableMerge();
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        sreq1 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("Park"), 0, null);
        sr1 = new ServerResponse(sreq1, null, null, null, null, null, null, null, null, null);
        sreq2 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("Band"), 0, null);
        sr2 = new ServerResponse(sreq2, null, null, null, null, null, null, null, null, null);
        
        // Call enable after 2nd disable - ensure no additions and no changes
        System.out.println("\n Extra call to enable after 2nd disable again");
        valve.enableMerge();
        valve.addPrimaryInput(sr1);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(9, testResponses.getOnNextEvents().size());
        
        // Call enable after 2nd disable - ensure no additions and no changes
        System.out.println("\n 2nd Extra call to enable after 2nd disable again");
        valve.addSecondaryInput(sr2);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(10, testResponses.getOnNextEvents().size());
        
        // Another call to swap
        System.out.println("\n Another call to swap");
        valve.swap();
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(11, testResponses.getOnNextEvents().size());
        assertEquals("Band", ((Term)valve.lastPrimary().getRequest().getModel()).getTerm());
        assertEquals("Park", ((Term)valve.lastSecondary().getRequest().getModel()).getTerm());
        
        // The swap() method sends a "reversed" combined output through so we 
        // should have another result...
        List<CompareResponse> results = testResponses.getOnNextEvents();
        assertEquals(11, results.size());
        
        checkErrors(testResponses, primaryResponses, secondaryResponses);
    }
    
    @Test
    public void testWorksWithTransformers() {
        TestObserver<CompareResponse> testResponses;
        TestObserver<StringBuilder> primaryResponses;
        TestObserver<StringBuilder> secondaryResponses;
        
        MergeSupplier<ServerResponse, CompareResponse, StringBuilder> ms = 
            new MergeSupplier<ServerResponse, CompareResponse, StringBuilder>(
                ServerResponse.class, CompareResponse.class, StringBuilder.class, 2, 
                    (Object[] oa) -> new CompareResponse(
                        ((ServerResponse)oa[0]).getRequest(), ((ServerResponse)oa[1]).getRequest(), null, null, null, null),
                t -> t.map(r -> {
                    StringBuilder retType = null;
                    try {
                        retType = new StringBuilder(r.getRequest().getModel().toJson());
                    }catch(Exception e) { e.printStackTrace(); }
                    return retType;
                }));
        
        MergeOrPassthruValve<ServerResponse, CompareResponse, StringBuilder> pl = new MergeOrPassthruValve<>(ms);
        
        pl.setMergeObserver(testResponses = new TestObserver<CompareResponse>() {
            public void onNext(CompareResponse r) {
                super.onNext(r);
                try {
                    System.out.println("SINK: \n\t" + r.getPrimaryRequest().getModel().toJson() +
                        "\n\t" + r.getSecondaryRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        pl.setPrimaryObserver(primaryResponses = new TestObserver<StringBuilder>() {
            public void onNext(StringBuilder r) {
                super.onNext(r);
                try {
                    System.out.println("PRIMARY: \n\t" + r.toString());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        pl.setSecondaryObserver(secondaryResponses = new TestObserver<StringBuilder>() {
            public void onNext(StringBuilder r) {
                super.onNext(r);
                try {
                    System.out.println("SECONDARY: \n\t" + r.toString());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        
        ServerRequest sreq1 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("car"), 0, null);
        ServerRequest sreq2 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("truck"), 0, null);
        ServerResponse sr1 = new ServerResponse(sreq1, null, null, null, null, null, null, null, null, null);
        ServerResponse sr2 = new ServerResponse(sreq2, null, null, null, null, null, null, null, null, null);
        
        // First test
        System.out.println("\n==================\nTransform Test: Merge with no transform on passthru");
        pl.addPrimaryInput(sr1);
        assertEquals(0, testResponses.getOnNextEvents().size());
        pl.addSecondaryInput(sr2);
        assertEquals(1, testResponses.getOnNextEvents().size());
        
        System.out.println("\nTransform Test: Transform passthru first response");
        pl.disableMerge();
        pl.addPrimaryInput(sr1);
        assertEquals(1, primaryResponses.getOnNextEvents().size());
        assertEquals(0, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(1, testResponses.getOnNextEvents().size()); 
        
        pl.addSecondaryInput(sr2);
        assertEquals(1, primaryResponses.getOnNextEvents().size());
        assertEquals(1, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(1, testResponses.getOnNextEvents().size()); 
        
        checkErrors(testResponses, primaryResponses, secondaryResponses);
    }
    
    @Test
    public void testValveUsingProperties() {
        TestObserver<CompareResponse> testResponses;
        TestObserver<ServerResponse> primaryResponses;
        TestObserver<ServerResponse> secondaryResponses;
        
        MergeSupplier<ServerResponse, CompareResponse, ServerResponse> ms = new MergeSupplier<ServerResponse, CompareResponse, ServerResponse>(
            ServerResponse.class, CompareResponse.class, ServerResponse.class, 2, 
                (Object[] oa) -> new CompareResponse(((ServerResponse)oa[0]).getRequest(), ((ServerResponse)oa[1]).getRequest(), null, null, null, null));
        
        MergeOrPassthruValve<ServerResponse, CompareResponse, ServerResponse> pl = new MergeOrPassthruValve<>(ms);
        
        pl.setMergeObserver(testResponses = new TestObserver<CompareResponse>() {
            public void onNext(CompareResponse r) {
                super.onNext(r);
                try {
                    System.out.println("SINK: \n\t" + r.getPrimaryRequest().getModel().toJson() +
                        "\n\t" + r.getSecondaryRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        pl.setPrimaryObserver(primaryResponses = new TestObserver<ServerResponse>() {
            public void onNext(ServerResponse r) {
                super.onNext(r);
                try {
                    System.out.println("PRIMARY: \n\t" + r.getRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        pl.setSecondaryObserver(secondaryResponses = new TestObserver<ServerResponse>() {
            public void onNext(ServerResponse r) {
                super.onNext(r);
                try {
                    System.out.println("SECONDARY: \n\t" + r.getRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        
        ServerRequest sreq1 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("car"), 0, null);
        ServerRequest sreq2 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("truck"), 0, null);
        ServerResponse sr1 = new ServerResponse(sreq1, null, null, null, null, null, null, null, null, null);
        ServerResponse sr2 = new ServerResponse(sreq2, null, null, null, null, null, null, null, null, null);
        
        // First test
        System.out.println("\nFirst Test");
        pl.primaryInputProperty().set(sr1);
        assertEquals(0, testResponses.getOnNextEvents().size());
        pl.secondaryInputProperty().set(sr2);
        assertEquals(1, testResponses.getOnNextEvents().size());
        
        // Second Test
        System.out.println("\nSecond Test");
        sreq2.setModel(new Term("pizza"));
        pl.secondaryInputProperty().set(sr2);
        assertEquals(2, testResponses.getOnNextEvents().size());
        
        // Third test
        System.out.println("\nThird Test");
        sreq1.setModel(new Term("airplane"));
        pl.primaryInputProperty().set(sr1);
        assertEquals(3, testResponses.getOnNextEvents().size());
        
        // Test that swap() will emit another combined input with 1 & 2 swapped
        System.out.println("\nSwap Test");
        pl.inputSwapProperty().set();
        assertEquals(4, testResponses.getOnNextEvents().size());
        
        //////////
        
        // Fifth test
        System.out.println("\nFifth Test");
        pl.primaryInputProperty().set(sr1);
        assertEquals(0, primaryResponses.getOnNextEvents().size());
        assertEquals(0, secondaryResponses.getOnNextEvents().size());
        assertEquals(5, testResponses.getOnNextEvents().size());
        
        // Sixth test
        System.out.println("\nSixth Test");
        pl.secondaryInputProperty().set(sr2);
        assertEquals(0, primaryResponses.getOnNextEvents().size());
        assertEquals(0, secondaryResponses.getOnNextEvents().size());
        assertEquals(6, testResponses.getOnNextEvents().size());
        
        // Seventh test
        System.out.println("\nSeventh Test");
        pl.mergeEnabledProperty().set(false);
        pl.primaryInputProperty().set(sr1);
        assertEquals(1, primaryResponses.getOnNextEvents().size());
        assertEquals(0, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(6, testResponses.getOnNextEvents().size()); 
        
        // Eighth test
        System.out.println("\nEighth Test");
        pl.secondaryInputProperty().set(sr2);
        assertEquals(1, primaryResponses.getOnNextEvents().size());
        assertEquals(1, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(6, testResponses.getOnNextEvents().size());
        
        // Test swap (The Pipeline is disabled so no changes should occur to outputs)
        System.out.println("\nDisabled Swap Test");
        pl.inputSwapProperty().set();
        assertEquals(1, primaryResponses.getOnNextEvents().size());
        assertEquals(1, secondaryResponses.getOnNextEvents().size());
        assertEquals(6, testResponses.getOnNextEvents().size());
        
        // Add a few more responses while disabled
        System.out.println("\nMore disabled Tests");
        pl.primaryInputProperty().set(sr1);
        pl.primaryInputProperty().set(sr1);
        pl.secondaryInputProperty().set(sr2);
        assertEquals(3, primaryResponses.getOnNextEvents().size());
        assertEquals(2, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(6, testResponses.getOnNextEvents().size());
        
        // Re-enable Pipeline test 1
        System.out.println("\n Re-enable pipeline 1");
        pl.mergeEnabledProperty().set(true);
        pl.secondaryInputProperty().set(sr2);
        assertEquals(3, primaryResponses.getOnNextEvents().size());
        assertEquals(2, secondaryResponses.getOnNextEvents().size());
        assertEquals(7, testResponses.getOnNextEvents().size());
        
        // Re-enable Pipeline test 2
        System.out.println("\n Re-enable pipeline 2");
        pl.primaryInputProperty().set(sr1);
        assertEquals(3, primaryResponses.getOnNextEvents().size());
        assertEquals(2, secondaryResponses.getOnNextEvents().size());
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        // Try disabling again
        System.out.println("\n 2nd Disable test");
        pl.mergeEnabledProperty().set(false);
        pl.primaryInputProperty().set(sr1);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(2, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        // Disabled 2nd input again
        System.out.println("\n Disabled 2nd input again");
        pl.secondaryInputProperty().set(sr2);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(3, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        // Disabled 2nd input again
        System.out.println("\n Disabled 2nd input another time");
        // Make sure calling disable a second time has no effect
        pl.mergeEnabledProperty().set(false); 
        pl.secondaryInputProperty().set(sr2);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        // Call enable after 2nd disable - ensure no additions and no changes
        System.out.println("\n Call enable after 2nd disable");
        pl.mergeEnabledProperty().set(true);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(8, testResponses.getOnNextEvents().size());
        
        sreq1 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("Park"), 0, null);
        sr1 = new ServerResponse(sreq1, null, null, null, null, null, null, null, null, null);
        sreq2 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("Band"), 0, null);
        sr2 = new ServerResponse(sreq2, null, null, null, null, null, null, null, null, null);
        
        // Call enable after 2nd disable - ensure no additions and no changes
        System.out.println("\n Extra call to enable after 2nd disable again");
        pl.mergeEnabledProperty().set(true);
        pl.primaryInputProperty().set(sr1);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(9, testResponses.getOnNextEvents().size());
        
        // Call enable after 2nd disable - ensure no additions and no changes
        System.out.println("\n 2nd Extra call to enable after 2nd disable again");
        pl.secondaryInputProperty().set(sr2);
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(10, testResponses.getOnNextEvents().size());
        
        // Another call to swap
        System.out.println("\n Another call to swap");
        pl.inputSwapProperty().set();
        assertEquals(4, primaryResponses.getOnNextEvents().size());
        assertEquals(4, secondaryResponses.getOnNextEvents().size());
        // Make sure the pipeline combination doesn't add emissions
        assertEquals(11, testResponses.getOnNextEvents().size());
        
        // The swap() method sends a "reversed" combined output through so we 
        // should have another result...
        List<CompareResponse> results = testResponses.getOnNextEvents();
        assertEquals(11, results.size());
        
        checkErrors(testResponses, primaryResponses, secondaryResponses);
    }
    
    @Test
    public void testStartUpCase() {
        TestObserver<CompareResponse> testResponses;
        TestObserver<ServerResponse> primaryResponses;
        TestObserver<ServerResponse> secondaryResponses;
        
        MergeSupplier<ServerResponse, CompareResponse, ServerResponse> ms = new MergeSupplier<ServerResponse, CompareResponse, ServerResponse>(
            ServerResponse.class, CompareResponse.class, ServerResponse.class, 2, 
                (Object[] oa) -> {
                    System.out.println("oa = " + oa + ", oa.length = " + oa.length);
                    System.out.println("oa[0] = " + oa[0]);
                    System.out.println("oa[1] = " + oa[1]);
                    return new CompareResponse(((ServerResponse)oa[0]).getRequest(), ((ServerResponse)oa[1]).getRequest(), null, null, null, null);
                });
        
        MergeOrPassthruValve<ServerResponse, CompareResponse, ServerResponse> pl = new MergeOrPassthruValve<>(ms);
        
        pl.setMergeObserver(testResponses = new TestObserver<CompareResponse>() {
            public void onNext(CompareResponse r) {
                super.onNext(r);
                try {
                    System.out.println("SINK: \n\t" + r.getPrimaryRequest().getModel().toJson() +
                        "\n\t" + r.getSecondaryRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        pl.setPrimaryObserver(primaryResponses = new TestObserver<ServerResponse>() {
            public void onNext(ServerResponse r) {
                super.onNext(r);
                try {
                    System.out.println("PRIMARY: \n\t" + r.getRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        pl.setSecondaryObserver(secondaryResponses = new TestObserver<ServerResponse>() {
            public void onNext(ServerResponse r) {
                super.onNext(r);
                try {
                    System.out.println("SECONDARY: \n\t" + r.getRequest().getModel().toJson());
                }catch(Exception e) { e.printStackTrace(); }
            }
        });
        
        // This is the start condition
        pl.disableMerge();
        
        ServerRequest sreq1 = new ServerRequest(0, 0, 0, PosType.ANY, null, new Term("car"), 0, null);
        ServerResponse sr1 = new ServerResponse(sreq1, null, null, null, null, null, null, null, null, null);
        
        // Enabling and submitting the first entry USED to cause an error to be thrown
        System.out.println("\nFirst Test");
        pl.enableMerge();
        pl.addPrimaryInput(sr1);
        
        checkErrors(testResponses, primaryResponses, secondaryResponses);
    }
    
    private void checkErrors(TestObserver<?> c, TestObserver<?> p, TestObserver<?> s) {
        if(c.getOnErrorEvents().size() > 0) {
            System.out.println("CompareObserver had the following errors:\n");
            c.getOnErrorEvents().stream().forEach(e -> e.printStackTrace());
        }
        
        if(c.getOnErrorEvents().size() > 0) System.out.println("=========");
        
        if(p.getOnErrorEvents().size() > 0) {
            System.out.println("PrimaryObserver had the following errors:\n");
            p.getOnErrorEvents().stream().forEach(e -> e.printStackTrace());
        }
        
        if(p.getOnErrorEvents().size() > 0) System.out.println("=========");
        
        if(s.getOnErrorEvents().size() > 0) {
            System.out.println("SecondaryObserver had the following errors:\n");
            s.getOnErrorEvents().stream().forEach(e -> e.printStackTrace());
        }
        
        // Fail the test if nested errors were 
        if(c.getOnErrorEvents().size() > 0 || p.getOnErrorEvents().size() > 0 || s.getOnErrorEvents().size() > 0) {
            fail();
        }
    }
}
