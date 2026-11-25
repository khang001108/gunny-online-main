/** Local display timing, independent of server settlement and network replay delay. */
final class ResultTimingHF15 {
 static final long SUMMARY_MS=3000L,CARDS_MS=10000L;
 private long summaryAt=-1,cardsAt=-1;
 void reset(){summaryAt=cardsAt=-1;}
 void summaryPainted(long now){if(summaryAt<0)summaryAt=now;}
 boolean summaryDone(long now){return summaryAt>=0&&now-summaryAt>=SUMMARY_MS;}
 void cardsPainted(long now){if(cardsAt<0)cardsAt=now;}
 long cardDeadline(long serverDeadline){return cardsAt<0?Long.MAX_VALUE:Math.max(serverDeadline,cardsAt+CARDS_MS);}
}
