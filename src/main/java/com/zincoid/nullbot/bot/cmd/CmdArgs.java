package com.zincoid.nullbot.bot.cmd;

import com.zincoid.nullbot.module.i18n.Text;
import com.zincoid.nullbot.platform.persistence.enums.Language;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 指令参数解析结果：选项与位置参数。
 * <p>Parsed command arguments: options and positional values.</p>
 */
@Getter
@SuppressWarnings("unused")
public final class CmdArgs {

    private final List<String> raw;
    private final Map<String, String> opt;
    private final List<String> pos;
    private final Language language;
    private int cursor;

    private CmdArgs(List<String> raw, Map<String, String> opt, List<String> pos, Language language) {
        this.raw = List.copyOf(raw);
        this.opt = Collections.unmodifiableMap(opt);
        this.pos = Collections.unmodifiableList(pos);
        this.language = language == null ? Language.ZH : language;
    }

    /**
     * 原始参数解析。选项（--flag、-f、--key=value、-k=v）被提取并从位置参数中删除。
     * <p>Parses raw params. Options (--flag, -f, --key=value, -k=v) are extracted and removed from positional.</p>
     *
     * @param params the raw parameters
     */
    public static CmdArgs of(List<String> params) {
        return of(params, Language.ZH);
    }

    /**
     * 原始参数解析，并绑定错误提示使用的语言。
     * <p>Parses raw params and binds the language used by error messages.</p>
     *
     * @param params   the raw parameters
     * @param language the reply language
     */
    public static CmdArgs of(List<String> params, Language language) {
        if (params.isEmpty())
            return new CmdArgs(List.of(), Map.of(), List.of(), language);
        Map<String, String> opt = new LinkedHashMap<>();
        List<String> pos = new ArrayList<>();
        for (String p : params) if (!tryParseOpt(p, opt)) pos.add(p);
        return new CmdArgs(params, opt, pos, language);
    }

    // ════ Option access ═══════════

    public boolean hasOpt(String name, String alias) {
        return opt.containsKey(name) ||
                alias != null && opt.containsKey(alias);
    }

    public String getOpt(String name, String alias) {
        String v = opt.get(name);
        if (v != null) return v;
        if (alias != null) {
            v = opt.get(alias);
            if (v != null) return v;
        }
        throw missingOpt(name, alias);
    }

    public String getOpt(String name, String alias, String or) {
        String v = opt.get(name);
        if (v != null) return v;
        if (alias != null) {
            v = opt.get(alias);
            if (v != null) return v;
        }
        return or;
    }

    public int optInt(String name, String alias) {
        try {
            return Integer.parseInt(getOpt(name, alias));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public int optInt(String name, String alias, int or) {
        String v = getOpt(name, alias, null);
        if (v == null) return or;
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public long optLong(String name, String alias) {
        try {
            return Long.parseLong(getOpt(name, alias));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public long optLong(String name, String alias, long or) {
        String v = getOpt(name, alias, null);
        if (v == null) return or;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public double optDouble(String name, String alias) {
        try {
            return Double.parseDouble(getOpt(name, alias));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public double optDouble(String name, String alias, double or) {
        String v = getOpt(name, alias, null);
        if (v == null) return or;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    // ════ Positional iterator ═════

    public boolean hasNext() {
        return cursor < pos.size();
    }

    public int size() {
        return pos.size();
    }

    public boolean isEmpty() {
        return pos.isEmpty();
    }

    public String peek() {
        return get(cursor);
    }

    public String next() {
        String v = get(cursor);
        cursor++;
        return v;
    }

    public String next(String or) {
        if (!hasNext()) return or;
        return pos.get(cursor++);
    }

    public String rest() {
        if (cursor >= pos.size()) throw missingArg();
        String result = String.join(" ", pos.subList(cursor, pos.size()));
        cursor = pos.size();
        return result;
    }

    public String rest(String or) {
        if (cursor >= pos.size()) return or;
        String result = String.join(" ", pos.subList(cursor, pos.size()));
        cursor = pos.size();
        return result;
    }

    public int nextInt() {
        String v = next();
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public int nextInt(int or) {
        if (!hasNext()) return or;
        try {
            return Integer.parseInt(pos.get(cursor++));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public long nextLong() {
        String v = next();
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public long nextLong(long or) {
        if (!hasNext()) return or;
        try {
            return Long.parseLong(pos.get(cursor++));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public double nextDouble() {
        String v = next();
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public double nextDouble(double or) {
        if (!hasNext()) return or;
        try {
            return Double.parseDouble(pos.get(cursor++));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    // ════ Positional indexed ══════

    public String get(int index) {
        if (index >= pos.size()) throw missingArg();
        return pos.get(index);
    }

    public String get(int index, String or) {
        return index < pos.size() ? pos.get(index) : or;
    }

    public String rest(int index) {
        if (index >= pos.size()) throw missingArg();
        return String.join(" ", pos.subList(index, pos.size()));
    }

    public String rest(int index, String or) {
        if (index >= pos.size()) return or;
        return String.join(" ", pos.subList(index, pos.size()));
    }

    public int getInt(int index) {
        try {
            return Integer.parseInt(get(index));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public int getInt(int index, int or) {
        if (index >= pos.size()) return or;
        try {
            return Integer.parseInt(pos.get(index));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public long getLong(int index) {
        try {
            return Long.parseLong(get(index));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public long getLong(int index, long or) {
        if (index >= pos.size()) return or;
        try {
            return Long.parseLong(pos.get(index));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public double getDouble(int index) {
        try {
            return Double.parseDouble(get(index));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    public double getDouble(int index, double or) {
        if (index >= pos.size()) return or;
        try {
            return Double.parseDouble(pos.get(index));
        } catch (NumberFormatException e) {
            throw formatError();
        }
    }

    // ════ Placeholder ═════════════

    public CmdArgs resolve(Map<String, String> vars) {
        return CmdArgs.of(raw.stream().map(s -> {
            for (Map.Entry<String, String> e : vars.entrySet())
                s = s.replace("{" + e.getKey() + "}", e.getValue());
            return s;
        }).toList(), language);
    }

    // ════ Helpers ═════════════════

    private CmdWarnException missingArg() {
        return new CmdWarnException(Text.ARG_MISSING.of(language));
    }

    private CmdWarnException formatError() {
        return new CmdWarnException(Text.ARG_MALFORMED.of(language));
    }

    private CmdWarnException missingOpt(String name, String alias) {
        return new CmdWarnException(alias != null
                ? Text.ARG_MISSING_OPTION.of(language, name, alias)
                : Text.ARG_MISSING_OPTION_ALIAS.of(language, name));
    }

    private static boolean tryParseOpt(String p, Map<String, String> opt) {
        if (p.startsWith("--")) {
            String body = p.substring(2);
            if (body.isEmpty() || body.startsWith("-")) return false;
            int eq = body.indexOf('=');
            if (eq < 0) {
                opt.put(body, "true");
                return true;
            }
            String name = body.substring(0, eq);
            if (name.isEmpty()) return false;
            opt.put(name, body.substring(eq + 1));
            return true;
        }
        if (p.startsWith("-") && p.length() >= 2 &&
                Character.isLetter(p.charAt(1))) {
            String name = p.substring(1, 2);
            if (p.length() > 2 && p.charAt(2) == '=') {
                opt.put(name, p.substring(3));
                return true;
            }
            if (p.length() == 2) {
                opt.put(name, "true");
                return true;
            }
        }
        return false;
    }
}
