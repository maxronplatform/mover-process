package org.maxron.platform.moverprocess;

import brave.propagation.TraceContext;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class MoverProcessTraceContextHolder {
    private long traceIdHigh;
    private long traceId;
    private Long parentId;
    private long spanId;
    private Boolean sampled;
    private boolean debug;

    MoverProcessTraceContextHolder() {
    }

    MoverProcessTraceContextHolder(long traceIdHigh, long traceId, Long parentId, long spanId, Boolean sampled, boolean debug) {
        this.traceIdHigh = traceIdHigh;
        this.traceId = traceId;
        this.parentId = parentId;
        this.spanId = spanId;
        this.sampled = sampled;
        this.debug = debug;
    }

    static MoverProcessTraceContextHolder hold(TraceContext traceContext) {
        return new MoverProcessTraceContextHolder(traceContext.traceIdHigh(), traceContext.traceId(),
                traceContext.parentId(), traceContext.spanId(), traceContext.sampled(), traceContext.debug());
    }

    TraceContext toTraceContext() {
        return TraceContext.newBuilder()
                .traceIdHigh(traceIdHigh)
                .traceId(traceId)
                .parentId(parentId)
                .spanId(spanId)
                .sampled(sampled)
                .debug(debug)
                .build();
    }
}
