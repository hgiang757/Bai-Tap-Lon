package com.auction.server.service;

import com.auction.server.dao.ItemRepository;
import com.auction.server.exception.ApiException;
import com.auction.server.model.Item;
import com.auction.server.model.ItemFactory;
import java.util.List;
import java.util.UUID;

public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public Item createItem(
            String type,
            String name,
            String description,
            double basePrice,
            String imageUrl,
            String extraInfo) {
        String id = "ITM-" + UUID.randomUUID().toString().substring(0, 8);
        Item item = ItemFactory.createItem(type, id, name, description, basePrice, imageUrl, extraInfo);
        return itemRepository.save(item);
    }

    public Item getItemOrThrow(String itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ApiException(404, "ITEM_NOT_FOUND", "Không tìm thấy sản phẩm: " + itemId));
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public boolean deleteItem(String itemId) {
        return itemRepository.deleteById(itemId);
    }
}
