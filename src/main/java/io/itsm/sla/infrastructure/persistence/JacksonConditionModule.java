package io.itsm.sla.infrastructure.persistence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.itsm.sla.model.condition.*;

import java.io.IOException;

/**
 * Jackson module для сериализации/десериализации sealed классов Condition.
 * <p>
 * Использует полиморфную типизацию через type-поле в JSON.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SimpleCondition.class, name = "simple"),
    @JsonSubTypes.Type(value = AttributeCondition.class, name = "attribute"),
    @JsonSubTypes.Type(value = AndCondition.class, name = "and"),
    @JsonSubTypes.Type(value = OrCondition.class, name = "or"),
    @JsonSubTypes.Type(value = NotCondition.class, name = "not"),
    @JsonSubTypes.Type(value = TrueCondition.class, name = "true"),
    @JsonSubTypes.Type(value = InCondition.class, name = "in")
})
interface ConditionMixin {}

/**
 * Регистрация Jackson-модулей для полиморфной (де)сериализации.
 */
public class JacksonConditionModule extends SimpleModule {

    public JacksonConditionModule() {
        super("SLA-Condition-Module");
        setMixInAnnotation(Condition.class, ConditionMixin.class);

        // Кастомный сериализатор для AttributeCondition.Operator
        addSerializer(AttributeCondition.Operator.class, new JsonSerializer<>() {
            @Override
            public void serialize(AttributeCondition.Operator value,
                                   JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.name().toLowerCase());
            }
        });

        addDeserializer(AttributeCondition.Operator.class, new JsonDeserializer<>() {
            @Override
            public AttributeCondition.Operator deserialize(JsonParser p,
                                                           DeserializationContext ctxt) throws IOException {
                return AttributeCondition.Operator.valueOf(p.getValueAsString().toUpperCase());
            }
        });
    }
}
