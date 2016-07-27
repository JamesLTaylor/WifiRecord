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
        GlobalDataFragment.currentCenter = new ShoppingCenter(getContext().getResources(), "Greenstone");
        //Shop shop = GlobalDataFragment.shopDirectory.getShop("Art, Antiques, Curios & Gifts", "Spilhaus");
        Shop shop = GlobalDataFragment.currentCenter.getShopDirectory().getShop("Computer Mania");
        Position start = new Position(1295, 607, 1);
        Route route = GlobalDataFragment.currentCenter.getMallGraph().getRoute(start, shop);
        route.createDescription();

        float a = 10;
    }
}
