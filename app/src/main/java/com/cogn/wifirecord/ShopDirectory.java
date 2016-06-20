package com.cogn.wifirecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A listing of shops
 */
public class ShopDirectory {
    private Map<String, List<Shop>> directory;
    private List<Shop> allShops;
    
    public ShopDirectory(){
        directory = new HashMap<>();
        allShops = new ArrayList<>();
    }

    public void loadFromFile(InputStream inputStream) {
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String str;
            while ((str = in.readLine()) != null) {
                String[] cols = str.split(",", 7);
                String category = cols[6];
                if (category.substring(0,1).equals("\"")) category = category.substring(1, category.length()-1);
                String shopName = cols[0];
                String entranceName = cols[2];
                int level = Integer.parseInt(cols[5]);
                float x = Float.parseFloat(cols[3]);
                float y = Float.parseFloat(cols[4]);
                add(category, shopName, entranceName, level, x, y);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void add(String category, String shopName, String entranceName, int level, float x, float y)
    {
        List<Shop> shopList;
        if (!directory.containsKey(category)) {
            shopList = new ArrayList<>();
            directory.put(category, shopList);
        } else {
            shopList = directory.get(category);
        }
        Shop shop = getShop(shopList, shopName);
        shop.add(shopName, level, x, y, entranceName);
    }

    public Shop getShop(String category, String shopName)
    {
        return getShop(directory.get(category), shopName);
    }


    /**
     * Get a shops based only only on the name.  Will return null if a match is not found.
     */
    public Shop getShop(String shopName){

        for (String key : directory.keySet()) {
            List<Shop> shopList = directory.get(key);
            for (Shop shop : shopList) {
                if (shop.getName().equals(shopName)) return shop;
            }
        }
        return null;
    }


    public List<Shop> getAllShops() {
        return allShops;
    }

    /**
     * Returns an existing shop or adds a new one to the list and returns that
     */
    private Shop getShop(List<Shop> shopList, String name)
    {
        for (Shop shop : shopList) {
            if (shop.getName().equals(name)) return shop;
        }
        shopList.add(new Shop());
        allShops.add(shopList.get(shopList.size()-1));
        return shopList.get(shopList.size()-1);
    }


    
    public String[] listCategories()
    {
        Set<String> categories = directory.keySet();
        String[] categoryArray = new String[categories.size()];
        return categories.toArray(categoryArray);
    }

    public String[] listShopNames(String category)
    {
        List<Shop> shopList = directory.get(category);
        String[] shopNames = new String[shopList.size()];
        for (int i = 0; i < shopList.size(); i++) {
            shopNames[i] = shopList.get(i).getName();
        }
        return shopNames;
    }


}
