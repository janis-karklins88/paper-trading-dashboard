package com.jk.paper_trading_dashboard.alpaca;

import com.jk.paper_trading_dashboard.alpaca.dto.BrokerOrderRequest;
import com.jk.paper_trading_dashboard.alpaca.dto.BrokerOrderResponse;

public interface BrokerClient {
    BrokerOrderResponse placeOrder(BrokerOrderRequest request);
    BrokerOrderResponse getOrder(String brokerOrderId);
    void cancelOrder(String brokerOrderId);
}
