import java.io.*;
import java.util.*;
class Desiarilize
{
    public static void main(String[] args)
    {
        String filename = "Results.obj";

        // Deserialization 
        try
        {
            // Reading the object from a file 
            FileInputStream file = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(file);

            // Method for deserialization of object 
            List<HashMap<String,Long>>object1 = (List<HashMap<String,Long>>)in.readObject();

            in.close();
            file.close();

            System.out.println("Object has been deserialized ");
            System.out.println("a = " + object1.toString());
            // System.out.println("b = " + object1.b); 
        }

        catch(IOException ex)
        {
            System.out.println("IOException is caught");
        }

        catch(ClassNotFoundException ex)
        {
            System.out.println("ClassNotFoundException is caught");
        }

    }
}