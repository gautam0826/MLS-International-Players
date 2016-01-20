import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class IntlPlayersWiki
{
    public static String player = "";
    public static String clubFrom = "";
    public static String clubTo = "";
    public static String playerURL = "";
    public static String clubFromURL = "";
    public static String clubToURL = "";
    public static String mode = "";
    public static String date = "";
    public static String position = "";
    public static String length = "";
    public static String nationality = "";
    public static Boolean nationalTeam = false;
    public static int numWords = 0;
    public static String league = "";
    public static int season = 0;
    public static FileWriter writer = null;
    
    public static void main(String[] args) throws IOException, ParseException
    {
        writer = new FileWriter("IntlPlayers.csv");
        writer.write("Season,Date,Player,Position,Club From,Club To,Transfer Mode,"
            + "Nationality,Intl Caps,Wikipedia Page Words,League From");
        for (season = 2009; season < 2016; season++)
        {
            scrapeData(season);
        }
    }
    
    public static void scrapeData(int year) throws IOException, ParseException
    {
        String url = "https://en.wikipedia.org/wiki/List_of_Major_League_Soccer_transfers_" + year;
        Document doc = Jsoup.connect( url ).timeout( 0 ).get();
        Element table = doc.select("table.wikitable").first();
        for (Element row : table.select("tr")) 
        {
            Elements tds = row.select("td");
            outerloop:
            if (tds.size() > 1)
            {
                clubToURL = getClubToURL(tds);
                mode = removeBrackets(tds.get(4).text());
                if (!getLeague(clubToURL).equals( "Major League Soccer" ) 
                                || mode.contains( "Trade" )
                                || mode.contains( "Draft" )
                                || mode.contains( "Homegrown" )
                                || mode.contains( "Waiver" ))
                {
                    break outerloop;
                }
                clubFromURL = getClubFromURL(tds);
                playerURL = getPlayerURL(tds);
                date = convertDate(removeBrackets(tds.get(0).text()));
                player = tds.get(1).text().replace( ",", "" );
                clubFrom = tds.get(2).text();
                clubTo = tds.get(3).text();
                nationality = getNation(tds);
                if (clubFrom.contains( "Academy" ))
                {
                    break outerloop;
                }
                if (playerURL.contains( "&action=edit&redlink=1" ))
                {
                    nationalTeam = false;
                    numWords = 0;
                    position = "?";
                    length = "?";
                }
                else
                {
                    nationalTeam = getNatsCallUp(playerURL);
                    numWords = getNumWords(playerURL);
                    //position = getPosition(playerURL);
                    //length = getLength(playerURL);
                }
                if (clubFromURL == null)
                {
                    league = "Unattached";
                }
                else if (clubFromURL.contains( "&action=edit&redlink=1" ))
                {
                    league = "Unknown";
                }
                else
                {
                    league = removeBrackets(getLeague(clubFromURL));
                }
                if (league.equals( "Major League Soccer" ))
                {
                    break outerloop;
                }
                //System.out.println(length);
                writeOut();
            }
        }
        writer.flush();
    }
    
    public static String getLeague(String url) throws IOException
    {
        String league = "Unknown";
        Document doc = Jsoup.connect( url ).timeout( 0 ).get();
        Element table = doc.select("table[class=infobox vcard]").first();
        for (Element row : table.select("tr")) 
        {
            Elements ths = row.select("th");
            if (ths.text().equals( "League" ))
            {
                Elements tds = row.select("td");
                league = tds.text();
            }
        }
        return league;
    }
    
//    public static String getPosition(String url) throws IOException
//    {
//        String position = "?";
//        Document doc = Jsoup.connect( url ).timeout( 0 ).get();
//        Element table = doc.select("table[class=infobox vcard]").first();
//        for (Element row : table.select("tr")) 
//        {
//            Elements ths = row.select("th");
//            if (ths.text().toLowerCase().contains( "position" ))
//            {
//                Elements tds = row.select("td");
//                position = tds.text();
//            }
//        }
//        return position;
//    }
    
    public static String getNation(Elements tds)
    {
        String nation = tds.get(1).select("a[href]").attr("abs:href");
        return nation.replace("https://en.wikipedia.org/wiki/", "").replace( "_", " " );
    }
    
    public static boolean getNatsCallUp(String url) throws IOException
    {
        Document doc = Jsoup.connect( url ).timeout( 0 ).get();
        Elements links = doc.select("a[href]");
        for (Element link : links) 
        {
            if (link.select("a[href]").attr("abs:href").contains("_national_football_team")
                            || link.select("a[href]").attr("abs:href").contains("_national_soccer_team"))
            {
                return true;
            }
        }
        return false;
    }
    
    public static String getLength(String url) throws IOException
    {
        String length = "Unknown";
        Document doc = Jsoup.connect( url ).timeout( 0 ).get();
        Element table = doc.select("table[class=infobox vcard]").first();
        boolean seniorCareer = false;
        for (Element row : table.select("tr")) 
        {
            Elements ths = row.select("th");
            Elements tds = row.select("td");
            String link = tds.select("a[href]").attr("abs:href");
            if (ths.text().toLowerCase().contains( "senior career" ))
            {
                seniorCareer = true;
            }
            if (seniorCareer && link.contains( "https://en.wikipedia.org/wiki/" ) 
                            && getLeague(link).equals( "Major League Soccer" ))
            {
                length = ths.text();
            }
        }
        return length;
    }
    
    public static int getNumWords(String url) throws IOException
    {
        Document doc = Jsoup.connect( url ).timeout( 0 ).get();
        String allwords = "";
        Elements paragraphs = doc.select( "p" );
        for ( Element e : paragraphs )
        {
            allwords += e.text();
        }
        return allwords.trim().split("\\s+").length;
    }
    
    public static String getPlayerURL(Elements tds)
    {
        int count = 0;
        Elements links = tds.get( 1 ).select("a[href]");
        for (Element link : links) 
        {
            if (count == 1)
            {
                return link.select("a[href]").attr("abs:href");
            }
            count++;
        }
        return null;
    }
    
    public static String getClubFromURL(Elements tds)
    {
        int num = 2;
        int count = 0;
        Elements links = tds.get( num ).select("a[href]");
        for (Element link : links) 
        {
            if (count == numURLS(tds, num) - 1)
            {
                return link.select("a[href]").attr("abs:href");
            }
            count++;
        }
        return null;
    }
    
    public static String getClubToURL(Elements tds)
    {
        int num = 3;
        int count = 0;
        Elements links = tds.get( num ).select("a[href]");
        for (Element link : links) 
        {
            if (count == numURLS(tds, num) - 1)
            {
                return link.select("a[href]").attr("abs:href");
            }
            count++;
        }
        return null;
    }
    
    @SuppressWarnings("unused")
    public static int numURLS(Elements tds, int num)
    {
        int count = 0;
        Elements links = tds.get( num ).select("a[href]");
        for (Element link : links) 
        {
            count++;
        }
        return count;
    }
    
    public static String removeBrackets(String str)
    {
        if (!str.contains( "[" ))
        {
            return str;
        }
        String[] strings = str.split("\\[");
        return strings[0];
    }
    
    public static void writeOut() throws IOException
    {
        writer.append('\n');
        writer.write( stringify() );
        writer.flush();
//        System.out.println(date);
//        System.out.println(player);
//        System.out.println(clubFrom);
//        System.out.println(clubTo);
//        System.out.println(mode);
//        System.out.println(nationality);
//        System.out.println(nationalTeam);
//        System.out.println(numWords);
//        System.out.println(league);
        System.out.println(playerURL);
        System.out.println();
    }
    
    public static String stringify()
    {
        String result = season + ",";
        result += date + ",";
        result += player + ",";
        result += position + ",";
        result += clubFrom + ",";
        result += clubTo + ",";
        result += mode + ",";
        result += nationality + ",";
        result += nationalTeam + ",";
        result += numWords + ",";
        result += league + ",";
        return result;
    }
    
    public static String convertDate(String dateInString) throws ParseException
    {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy");
        Date date = formatter.parse(dateInString);
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
        return df.format(date);
    }
}
