package com.cogn.wifirecord;

import android.test.AndroidTestCase;

import org.junit.Test;

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
        GlobalDataFragment.mallGraph = new Graph();
        GlobalDataFragment.mallGraph.loadFromFile(getContext().getResources().openRawResource(R.raw.greenstone_graph), 4.2);
        GlobalDataFragment.shopDirectory = new ShopDirectory();
        GlobalDataFragment.shopDirectory.loadFromFile(getContext().getResources().openRawResource(R.raw.shop_locations));
        //Shop shop = GlobalDataFragment.shopDirectory.getShop("Art, Antiques, Curios & Gifts", "Spilhaus");
        Shop shop = GlobalDataFragment.shopDirectory.getShop("Computer Mania");
        Position start = new Position(1295, 607, 1);
        Route route = GlobalDataFragment.mallGraph.getRoute(start, shop);
        route.createDescription();

        float a = 10;
    }
}
