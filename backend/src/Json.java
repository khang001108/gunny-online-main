import java.util.*;

/** Small strict JSON codec for the Supabase REST API; no runtime dependencies. */
final class Json {
    static Object parse(String text) { Parser p=new Parser(text);Object v=p.value(0);p.ws();if(p.i!=text.length())throw new IllegalArgumentException("Trailing JSON");return v; }
    @SuppressWarnings("unchecked") static Map<String,Object> obj(Object v){return (Map<String,Object>)v;}
    static String str(Map<String,Object> m,String k){Object v=m.get(k);return v==null?"":String.valueOf(v);}
    static int num(Map<String,Object> m,String k,int fallback){Object v=m.get(k);return v instanceof Number?((Number)v).intValue():fallback;}
    static String quote(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}}return b.append('"').toString();}
    static String write(Object v){if(v==null)return "null";if(v instanceof String)return quote((String)v);if(v instanceof Number||v instanceof Boolean)return v.toString();StringJoiner j=new StringJoiner(",",v instanceof Map?"{":"[",v instanceof Map?"}":"]");if(v instanceof Map){for(Object item:((Map<?,?>)v).entrySet()){Map.Entry<?,?> e=(Map.Entry<?,?>)item;j.add(quote(String.valueOf(e.getKey()))+":"+write(e.getValue()));}}else for(Object item:(Iterable<?>)v)j.add(write(item));return j.toString();}
    static final class Parser {
        final String s;int i;Parser(String s){this.s=s;}
        void ws(){while(i<s.length()&&Character.isWhitespace(s.charAt(i)))i++;}
        char take(){if(i>=s.length())throw new IllegalArgumentException("Truncated JSON");return s.charAt(i++);}
        Object value(int depth){if(depth>32)throw new IllegalArgumentException("JSON nesting");ws();char c=take();if(c=='"')return string();if(c=='{'||c=='['){boolean map=c=='{';Map<String,Object> m=new LinkedHashMap<>();List<Object> a=new ArrayList<>();ws();if(i<s.length()&&s.charAt(i)==(map?'}':']')){i++;return map?m:a;}while(true){if(map){ws();if(take()!='"')throw new IllegalArgumentException("JSON key");String key=string();ws();if(take()!=':')throw new IllegalArgumentException("JSON colon");m.put(key,value(depth+1));}else a.add(value(depth+1));ws();char end=take();if(end==(map?'}':']'))break;if(end!=',')throw new IllegalArgumentException("JSON comma");}return map?m:a;}if(c=='t'||c=='f'||c=='n'){String word=c=='t'?"true":c=='f'?"false":"null";if(!s.startsWith(word,i-1))throw new IllegalArgumentException("JSON literal");i+=word.length()-1;return c=='n'?null:c=='t';}int start=i-1;while(i<s.length()&&"0123456789.eE+-".indexOf(s.charAt(i))>=0)i++;return Double.valueOf(s.substring(start,i));}
        String string(){StringBuilder b=new StringBuilder();while(true){char c=take();if(c=='"')return b.toString();if(c=='\\'){char e=take();if(e=='u'){if(i+4>s.length())throw new IllegalArgumentException("JSON unicode");c=(char)Integer.parseInt(s.substring(i,i+4),16);i+=4;}else if(e=='n')c='\n';else if(e=='r')c='\r';else if(e=='t')c='\t';else if(e=='b')c='\b';else if(e=='f')c='\f';else if(e=='"'||e=='\\'||e=='/')c=e;else throw new IllegalArgumentException("JSON escape");}else if(c<32)throw new IllegalArgumentException("JSON control");b.append(c);}}
    }
}
