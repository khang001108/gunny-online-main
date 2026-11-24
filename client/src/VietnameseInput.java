/** Small CLDC-compatible Telex composer; no platform IME required. */
final class VietnameseInput {
    private static final String[] LOWER={"aàáảãạ","ăằắẳẵặ","âầấẩẫậ","eèéẻẽẹ","êềếểễệ","iìíỉĩị","oòóỏõọ","ôồốổỗộ","ơờớởỡợ","uùúủũụ","ưừứửữự","yỳýỷỹỵ"};
    private static final String[] UPPER={"AÀÁẢÃẠ","ĂẰẮẲẴẶ","ÂẦẤẨẪẬ","EÈÉẺẼẸ","ÊỀẾỂỄỆ","IÌÍỈĨỊ","OÒÓỎÕỌ","ÔỒỐỔỖỘ","ƠỜỚỞỠỢ","UÙÚỦŨỤ","ƯỪỨỬỮỰ","YỲÝỶỸỴ"};
    private static int group(char c){for(int i=0;i<LOWER.length;i++)if(LOWER[i].indexOf(c)>=0||UPPER[i].indexOf(c)>=0)return i;return -1;}
    private static int tone(char c,int g){int i=LOWER[g].indexOf(c);return i<0?UPPER[g].indexOf(c):i;}
    private static char vowel(char c,int g,int t){return (Character.isUpperCase(c)?UPPER[g]:LOWER[g]).charAt(t);}
    private static String set(String s,int i,char c){return s.substring(0,i)+c+s.substring(i+1);}
    private static boolean letter(char c){return group(c)>=0||c=='đ'||c=='Đ'||c>='a'&&c<='z'||c>='A'&&c<='Z';}
    private static int plain(int g){return g==1||g==2?0:g==4?3:g==7||g==8?6:g==10?9:g;}
    /** Return replacement for the text left of the cursor. Null means ordinary insertion. */
    static String compose(String prefix,char key){
        char k=Character.toLowerCase(key);int start=prefix.length();while(start>0&&letter(prefix.charAt(start-1)))start--;
        String word=prefix.substring(start);if(word.length()==0)return null;
        int n=word.length(),mark="fsrxj".indexOf(k)+1;
        if(mark>0||k=='z'){
            int[] pos=new int[n];int count=0,old=0;
            for(int i=0;i<n;i++){int g=group(word.charAt(i));if(g<0)continue;int t=tone(word.charAt(i),g);if(t>0)old=t;pos[count++]=i;}
            if(count==0)return null;
            if(k=='z'){boolean changed=false;for(int i=0;i<n;i++){char ch=word.charAt(i);int g=group(ch);if(g>=0&&(tone(ch,g)!=0||g!=plain(g))){word=set(word,i,vowel(ch,plain(g),0));changed=true;}else if(ch=='đ'||ch=='Đ'){word=set(word,i,ch=='đ'?'d':'D');changed=true;}}return changed?prefix.substring(0,start)+word:null;}
            int first=0;
            if(count>1&&pos[0]>0){char prev=Character.toLowerCase(word.charAt(pos[0]-1));int g=group(word.charAt(pos[0]));if(prev=='q'&&g==9||prev=='g'&&g==5)first++;}
            int target=pos[first];for(int i=first;i<count;i++){int g=group(word.charAt(pos[i]));if(g==1||g==2||g==4||g==7||g==8||g==10)target=pos[i];}
            if(plain(group(word.charAt(target)))==group(word.charAt(target))){int c=count-first;if(c>=3)target=pos[first+1];else if(c==2){target=pos[count-1]<n-1?pos[first+1]:pos[first];}}
            for(int i=0;i<count;i++){int p=pos[i],g=group(word.charAt(p));word=set(word,p,vowel(word.charAt(p),g,0));}
            if(old==mark)return prefix.substring(0,start)+word+key;
            word=set(word,target,vowel(word.charAt(target),group(word.charAt(target)),mark));return prefix.substring(0,start)+word;
        }
        if(k=='d'){char last=word.charAt(n-1);if(last=='d'||last=='D')return prefix.substring(0,prefix.length()-1)+(last=='D'?'Đ':'đ');if(last=='đ'||last=='Đ')return prefix.substring(0,prefix.length()-1)+(last=='Đ'?'D':'d')+key;}
        if(k!='a'&&k!='e'&&k!='o'&&k!='w')return null;
        for(int i=n-1;i>=0;i--){char ch=word.charAt(i);int g=group(ch),to=-1;if(g<0)continue;
            if(k=='a'&&(g==0||g==2))to=g==0?2:0;else if(k=='e'&&(g==3||g==4))to=g==3?4:3;else if(k=='o'&&(g==6||g==7))to=g==6?7:6;else if(k=='w'){if(g==0||g==1)to=g==0?1:0;else if(g==6||g==8)to=g==6?8:6;else if(g==9||g==10)to=g==9?10:9;}
            if(to<0)continue;if(k!='w'&&i!=n-1)return null;
            int t=tone(ch,g);word=set(word,i,vowel(ch,to,t));
            if(k=='w'&&to==8&&i>0&&group(word.charAt(i-1))==9){char u=word.charAt(i-1);word=set(word,i-1,vowel(u,10,tone(u,9)));}
            return prefix.substring(0,start)+word+(to==plain(to)?String.valueOf(key):"");
        }return null;
    }
}
