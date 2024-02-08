using System;
using System.Collections.Generic;
using System.IO;
using System.Reflection;
using Mono.Cecil;

namespace modloader
{
    public static class Patcher
    {
        public static IEnumerable<string> TargetDLLs => GetDLLs();

        public static IEnumerable<string> GetDLLs()
        {
            string codeBase = Assembly.GetExecutingAssembly().CodeBase;
            UriBuilder uri = new UriBuilder(codeBase);
            string path = Path.Combine(Path.GetDirectoryName(Uri.UnescapeDataString(uri.Path)), "replace");

            if (Directory.Exists(path))
            {
                foreach (string assembly in Directory.GetFiles(path, "*.dll", SearchOption.TopDirectoryOnly))
                {
                    Console.WriteLine("Found Assembly replacement at " + assembly);
                    string fileName = Path.GetFileName(assembly);
                    yield return fileName;
                }
            }
        }

        // Patches the assemblies
        public static void Patch(ref AssemblyDefinition assembly)
        {
            Console.WriteLine("Replacing " + assembly.FullName);

            string codeBase = Assembly.GetExecutingAssembly().CodeBase;
            UriBuilder uri = new UriBuilder(codeBase);
            string path = Path.Combine(Path.GetDirectoryName(Uri.UnescapeDataString(uri.Path)), "replace");
            if (Directory.Exists(path))
            {
                assembly = AssemblyDefinition.ReadAssembly(Path.Combine(path, assembly.Name.Name + ".dll"));
            }
            else
            {
                Console.WriteLine("Replacment path is gone??? No game Assembles will be patched!");
            }
        }
    }
}
