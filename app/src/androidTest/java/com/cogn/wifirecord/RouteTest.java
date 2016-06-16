package com.cogn.wifirecord;

import android.test.AndroidTestCase;
import android.test.InstrumentationTestCase;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

//@RunWith(JUnit4.class)
public class RouteTest extends AndroidTestCase{

    public RouteTest(){}

/*
    @Rule
    public ActivityTestRule mActivityRule = new ActivityTestRule<>(
            RecordActivity.class);
*/
    @Test
    public void testRoute(){
        GlobalData.mallGraph = new Graph();
        GlobalData.mallGraph.loadFromFile(getContext().getResources().openRawResource(R.raw.greenstone_graph));
        GlobalData.shopDirectory = new ShopDirectory();
        GlobalData.shopDirectory.loadFromFile(getContext().getResources().openRawResource(R.raw.shop_locations));
        Shop shop = GlobalData.shopDirectory.getShop("Art, Antiques, Curios & Gifts", "Spilhaus");
        //Shop shop = GlobalData.shopDirectory.getShop("Food", "Pick n Pay Hypermarket");
        Position start = new Position(657, 386, 1);
        Route route = GlobalData.mallGraph.getRoute(start, shop);
        float a = 10;
    }
}
