package me.towdium.pinin;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Keyboard {
    private static Map<String, String> DAQIAN_KEYS = Stream.of(new String[][]{
            {"", ""}, {"0", ""}, {"1", " "}, {"2", "6"}, {"3", "3"},
            {"4", "4"}, {"a", "8"}, {"ai", "9"}, {"an", "0"}, {"ang", ";"},
            {"ao", "l"}, {"b", "1"}, {"c", "h"}, {"ch", "t"}, {"d", "2"},
            {"e", "k"}, {"ei", "o"}, {"en", "p"}, {"eng", "/"}, {"er", "-"},
            {"f", "z"}, {"g", "e"}, {"h", "c"}, {"i", "u"}, {"ia", "u8"},
            {"ian", "u0"}, {"iang", "u;"}, {"iao", "ul"}, {"ie", "u,"}, {"in", "up"},
            {"ing", "u/"}, {"iong", "m/"}, {"iu", "u."}, {"j", "r"}, {"k", "d"},
            {"l", "x"}, {"m", "a"}, {"n", "s"}, {"o", "i"}, {"ong", "j/"},
            {"ou", "."}, {"p", "q"}, {"q", "f"}, {"r", "b"}, {"s", "n"},
            {"sh", "g"}, {"t", "w"}, {"u", "j"}, {"ua", "j8"}, {"uai", "j9"},
            {"uan", "j0"}, {"uang", "j;"}, {"uen", "mp"}, {"ueng", "j/"}, {"ui", "jo"},
            {"un", "jp"}, {"uo", "ji"}, {"v", "m"}, {"van", "m0"}, {"vang", "m;"},
            {"ve", "m,"}, {"vn", "mp"}, {"w", "j"}, {"x", "v"}, {"y", "u"},
            {"z", "y"}, {"zh", "5"},
    }).collect(Collectors.toMap(d -> d[0], d -> d[1]));

    private static Map<String, String> XIAOHE_KEYS = Stream.of(new String[][]{
            {"ai", "d"}, {"an", "j"}, {"ang", "h"}, {"ao", "c"}, {"ch", "i"},
            {"ei", "w"}, {"en", "f"}, {"eng", "g"}, {"ia", "x"}, {"ian", "m"},
            {"iang", "l"}, {"iao", "n"}, {"ie", "p"}, {"in", "b"}, {"ing", "k"},
            {"iong", "s"}, {"iu", "q"}, {"ong", "s"}, {"ou", "z"}, {"sh", "u"},
            {"ua", "x"}, {"uai", "k"}, {"uan", "r"}, {"uang", "l"}, {"ui", "v"},
            {"un", "y"}, {"uo", "o"}, {"ve", "t"}, {"ue", "t"}, {"vn", "y"},
            {"zh", "v"},
    }).collect(Collectors.toMap(d -> d[0], d -> d[1]));

    private static Map<String, String> ZIRANMA_KEYS = Stream.of(new String[][]{
            {"ai", "l"}, {"an", "j"}, {"ang", "h"}, {"ao", "k"}, {"ch", "i"},
            {"ei", "z"}, {"en", "f"}, {"eng", "g"}, {"ia", "w"}, {"ian", "m"},
            {"iang", "d"}, {"iao", "c"}, {"ie", "x"}, {"in", "n"}, {"ing", "y"},
            {"iong", "s"}, {"iu", "q"}, {"ong", "s"}, {"ou", "b"}, {"sh", "u"},
            {"ua", "w"}, {"uai", "y"}, {"uan", "r"}, {"uang", "d"}, {"ui", "v"},
            {"un", "p"}, {"uo", "o"}, {"ve", "t"}, {"ue", "t"}, {"vn", "p"},
            {"zh", "v"},
    }).collect(Collectors.toMap(d -> d[0], d -> d[1]));

    private static Map<String, String> PHONETIC_LOCAL = Stream.of(new String[][]{
            {"yi", "i"}, {"you", "iu"}, {"yin", "in"}, {"ye", "ie"}, {"ying", "ing"},
            {"wu", "u"}, {"wen", "un"}, {"yu", "v"}, {"yue", "ve"}, {"yuan", "van"},
            {"yun", "vn"}, {"ju", "jv"}, {"jue", "jve"}, {"juan", "jvan"}, {"jun", "jvn"},
            {"qu", "qv"}, {"que", "qve"}, {"quan", "qvan"}, {"qun", "qvn"}, {"xu", "xv"},
            {"xue", "xve"}, {"xuan", "xvan"}, {"xun", "xvn"}, {"shi", "sh"}, {"si", "s"},
            {"chi", "ch"}, {"ci", "c"}, {"zhi", "zh"}, {"zi", "z"}, {"ri", "r"},
    }).collect(Collectors.toMap(d -> d[0], d -> d[1]));

    public static Keyboard QUANPIN = new Keyboard(null, null, Keyboard::standard, false);
    public static Keyboard DAQIAN = new Keyboard(PHONETIC_LOCAL, DAQIAN_KEYS, Keyboard::standard, false);
    public static Keyboard XIAOHE = new Keyboard(null, XIAOHE_KEYS, Keyboard::zero, true);
    public static Keyboard ZIRANMA = new Keyboard(null, ZIRANMA_KEYS, Keyboard::zero, true);

    final Map<String, String> local;
    final Map<String, String> keys;
    final Function<String, String[]> cutter;
    public final boolean duo;

    public Keyboard(Map<String, String> local, Map<String, String> keys,
                    Function<String, String[]> cutter, boolean duo) {
        this.local = local;
        this.keys = keys;
        this.cutter = cutter;
        this.duo = duo;
    }

    public static String[] standard(String s) {
        if (s.startsWith("a") || s.startsWith("e") || s.startsWith("i")
                || s.startsWith("o") || s.startsWith("u")) {
            return new String[]{s.substring(0, s.length() - 1), s.substring(s.length() - 1)};
        } else {
            int i = s.length() > 2 && s.charAt(1) == 'h' ? 2 : 1;
            if (s.length() == i + 1) return new String[]{s.substring(0, i), s.substring(s.length() - 1)};
            else return new String[]{s.substring(0, i), s.substring(i, s.length() - 1), s.substring(s.length() - 1)};
        }
    }

    public String keys(String s) {
        return keys == null ? s : keys.getOrDefault(s, s);
    }

    public static String[] zero(String s) {
        String[] ss = standard(s);
        if (ss.length != 3) {
            String vowel = ss[0];
            ss = new String[]{Character.toString(vowel.charAt(0)), ss[0], ss[0]};
            if (vowel.length() == 2) ss[1] = Character.toString(vowel.charAt(1));
        }
        return ss;
    }

    public String[] split(String s) {
        if (local != null) {
            String cut = s.substring(0, s.length() - 1);
            String alt = local.get(cut);
            if (alt != null) s = alt + s.charAt(s.length() - 1);
        }
        return cutter.apply(s);
    }
}
