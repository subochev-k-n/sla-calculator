package io.itsm.sla.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.itsm.sla.model.action.*;

/**
 * Jackson module для сериализации/десериализации sealed классов SLAAction.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DurationAction.class, name = "duration"),
    @JsonSubTypes.Type(value = FixedDeadlineAction.class, name = "fixed-deadline"),
    @JsonSubTypes.Type(value = EscalateAction.class, name = "escalate"),
    @JsonSubTypes.Type(value = CompositeAction.class, name = "composite")
})
interface ActionMixin {}

public class JacksonActionModule extends SimpleModule {

    public JacksonActionModule() {
        super("SLA-Action-Module");
        setMixInAnnotation(SLAAction.class, ActionMixin.class);
    }
}
