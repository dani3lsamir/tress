import app.tres.notes.*;
import java.util.*;

public class ScheduleTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        Notebook b=new Notebook(Collections.emptyList());long now=10000000000L;
        String id=b.add("Monthly",now);b.schedule(id,"2027-01-31","monthly",now);
        b.complete(id,now);Note feb=b.all().stream().filter(n->!n.done).findFirst().get();
        check(feb.due.equals("2027-02-28"));b.complete(feb.id,now);
        check(b.all().stream().anyMatch(n->n.due.equals("2027-03-31")));
        b.restore(id,now);b.complete(id,now);check(b.all().size()==3);
        b.edit(id,"Changed",now+100);check(b.find(id).completedAt==now);
        check(!b.cleanup(now+10L*86400000-1));check(b.cleanup(now+10L*86400000));
        check(b.all().size()==1&&!b.all().get(0).done);
        id=b.add("Leap",now);b.schedule(id,"2028-02-28","daily",now);b.complete(id,now);
        check(b.all().stream().anyMatch(n->n.due.equals("2028-02-29")));
        id=b.add("Weekly",now);b.schedule(id,"2026-12-29","weekly",now);b.complete(id,now);
        check(b.all().stream().anyMatch(n->n.due.equals("2027-01-05")));
        boolean rejected=false;try{b.schedule(id,"","daily",now);}catch(IllegalArgumentException e){rejected=true;}check(rejected);
        rejected=false;try{b.schedule(id,"2027-02-30","none",now);}catch(IllegalArgumentException e){rejected=true;}check(rejected);
        // Re-saving an unchanged monthly schedule while clamped (Feb 28) keeps the day-31 anchor.
        Notebook m=new Notebook(Collections.emptyList());String rent=m.add("Rent",now);m.schedule(rent,"2027-01-31","monthly",now);m.complete(rent,now);
        Note clamped=m.all().stream().filter(n->!n.done).findFirst().get();check(clamped.due.equals("2027-02-28")&&clamped.anchorDay==31);
        m.schedule(clamped.id,clamped.due,clamped.repeat,now+1);check(m.find(clamped.id).anchorDay==31&&m.find(clamped.id).updated==now);
        m.complete(clamped.id,now+2);check(m.all().stream().anyMatch(n->!n.done&&n.due.equals("2027-03-31")));
        // Changing the date does reset the anchor.
        String moved=m.all().stream().filter(n->!n.done).findFirst().get().id;m.schedule(moved,"2027-03-15","monthly",now+3);check(m.find(moved).anchorDay==15);
        // Stored anchor days outside 0..31 are rejected.
        rejected=false;try{new Notebook(Collections.singletonList(new Note("x","A",1,1,false,0,"2027-01-01","monthly",0,40,"")));}catch(IllegalArgumentException e){rejected=true;}check(rejected);
        System.out.println("PASS recurrence, month anchor, leap day, year boundary, restore idempotency, retention boundary, unchanged-schedule anchor and date validation");
    }
}
