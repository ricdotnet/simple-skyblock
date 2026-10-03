package dev.ricr.skyblock.enums;

import lombok.Getter;

public enum TransactionType {
    ShopBuy("Buy"),
    ShopSell("Sell"),
    AuctionHouseBuy("Buy"),
    AuctionHouseSell("Sell"),
    SignShopTradeIn("Trade In"),
    SignShopTradeOut("Trade Out");

    @Getter
    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public static TransactionType getByLabel(String label) {
        for (var type : TransactionType.values()) {
            if (type.getLabel().equals(label)) {
                return type;
            }
        }
        return null;
    }
}
