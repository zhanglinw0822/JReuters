package com.puxtech.reuters.rfa.RelayServer;

import com.lmax.disruptor.EventFactory;
import com.puxtech.reuters.rfa.Common.Quote;

public final class QuoteEvent
{
    private Quote value;

    public Quote getValue()
    {
        return value;
    }

    public void setValue(final Quote value)
    {
        this.value = value;
    }

    public final static EventFactory<QuoteEvent> EVENT_FACTORY = new EventFactory<QuoteEvent>()
    {
        public QuoteEvent newInstance()
        {
            return new QuoteEvent();
        }
    };
}
