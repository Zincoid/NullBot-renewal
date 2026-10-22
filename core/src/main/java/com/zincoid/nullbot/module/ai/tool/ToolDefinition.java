package com.zincoid.nullbot.module.ai.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 工具定义：名称、描述与 JSON Schema 参数。
 * <p>Tool definition: name, description, and JSON Schema parameters.</p>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolDefinition {

    private record ParamSpec(String name, String type, String description, Set<String> enumValues) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final String description;
    private final ObjectNode parameters;

    public static Builder builder(String name, String description) {
        return new Builder(name, description);
    }

    @RequiredArgsConstructor
    public static class Builder {

        private final String name;
        private final String description;
        private final List<ParamSpec> params = new ArrayList<>();
        private final Set<String> required = new LinkedHashSet<>();

        public Builder addString(String name, String desc) {
            params.add(new ParamSpec(name, "string", desc, null));
            return this;
        }

        public Builder addString(String name, String desc, boolean required) {
            params.add(new ParamSpec(name, "string", desc, null));
            if (required) this.required.add(name);
            return this;
        }

        public Builder addInteger(String name, String desc) {
            params.add(new ParamSpec(name, "integer", desc, null));
            return this;
        }

        public Builder addInteger(String name, String desc, boolean required) {
            params.add(new ParamSpec(name, "integer", desc, null));
            if (required) this.required.add(name);
            return this;
        }

        public Builder addEnum(String name, String desc, Set<String> values) {
            params.add(new ParamSpec(name, "string", desc, new TreeSet<>(values)));
            return this;
        }

        public Builder addEnum(String name, String desc, Set<String> values, boolean required) {
            params.add(new ParamSpec(name, "string", desc, new TreeSet<>(values)));
            if (required) this.required.add(name);
            return this;
        }

        /**
         * 构建工具定义。
         * <p>Builds the tool definition.</p>
         *
         * @return the tool definition
         */
        public ToolDefinition build() {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("type", "object");
            ObjectNode props = MAPPER.createObjectNode();
            for (ParamSpec spec : params) {
                ObjectNode prop = MAPPER.createObjectNode();
                prop.put("type", spec.type);
                prop.put("description", spec.description);
                if (spec.enumValues != null) {
                    ArrayNode arr = MAPPER.createArrayNode();
                    spec.enumValues.forEach(arr::add);
                    prop.set("enum", arr);
                }
                props.set(spec.name, prop);
            }
            root.set("properties", props);
            if (!required.isEmpty()) {
                ArrayNode requiredArr = MAPPER.createArrayNode();
                required.forEach(requiredArr::add);
                root.set("required", requiredArr);
            }
            return new ToolDefinition(name, description, root);
        }
    }
}
