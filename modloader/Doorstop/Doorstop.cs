using System.IO;

namespace Doorstop
{
    class Entrypoint
    {
        public static void Start()
        {
            File.WriteAllText("doorstop_hello.log", "Hello from Unity!");;
        }
    }
}