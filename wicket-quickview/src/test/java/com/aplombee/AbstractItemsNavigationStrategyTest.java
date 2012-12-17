package com.aplombee;

import org.apache.wicket.markup.repeater.IItemFactory;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.mockito.Mockito;
import org.testng.Assert;

import java.util.Iterator;

/**
 *  @author Vineet Semwal
 */
public class AbstractItemsNavigationStrategyTest {

    public void assertAddItems(IQuickReuseStrategy strategy){
        IModel model1= Mockito.mock(IModel.class);
        IModel model2=Mockito.mock(IModel.class);
        Iterator newModels=Mockito.mock(Iterator.class);
        Mockito.when(newModels.next()).thenReturn(model1).thenReturn(model2);
        Mockito.when(newModels.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        IItemFactory factory=Mockito.mock(IItemFactory.class);
       final int startIndex=345;
        final int index2=startIndex+1;
        Item item1=new Item("345",startIndex,model1) ;
        Mockito.when(factory.newItem(startIndex,model1)).thenReturn(item1);
        Item item2=new Item("346",index2,model2);
        Mockito.when(factory.newItem(index2,model2)).thenReturn(item2);

        Iterator<Item> actual=strategy.addItems(startIndex,factory,newModels);
        Mockito.verify(factory,Mockito.times(1)).newItem(startIndex,model1);
        Mockito.verify(factory,Mockito.times(1)).newItem(index2,model2);
        Assert.assertEquals(actual.next(),item1);
        Assert.assertEquals(actual.next(),item2);
        Assert.assertFalse(actual.hasNext());

    }

       public void assertIsAddItemsSupported(IQuickReuseStrategy strategy){
        Assert.assertTrue(strategy.isAddItemsSupported());
    }


}
