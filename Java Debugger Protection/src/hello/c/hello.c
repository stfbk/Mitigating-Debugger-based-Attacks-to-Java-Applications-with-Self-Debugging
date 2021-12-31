#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <jvmti.h>
#include <dlfcn.h>
#include <sys/types.h>
#include <unistd.h>
#include <errno.h>
#include <math.h>
#include <string.h>
#include <sys/mman.h>
#include <stdint.h>
#include <signal.h>

extern int errno;



// This method checks whether the library jdwp.so, needed for java debugging, is
// loaded in memory or not. If it is so, the function exits the VM. Otherwise,
// the function just returns
JNIEXPORT void JNICALL Java_Utility_simpleProtection(JNIEnv *env, jobject obj) {

    // one method to check whether the jdwp library is loaded or not is the following
    // ===== HOWEVER, IT DOES NOT WORK =====
    // RTLD_NOLOAD (http://man7.org/linux/man-pages/man3/dlopen.3.html)
    // Don't load the shared object. This can be used to test if the
    // object is already resident (dlopen() returns NULL if it is
    // not, or the object's handle if it is resident)
    //    if (dlopen("libjdwp.so", RTLD_NOLOAD) != NULL) {
    //
    //        printf("jdwp library is loaded, debugging is going on. Exiting...\n");
    //
    //        // if is it not null, it means that the library is loaded, i.e.
    //        // there is a java debugger going on. Therefore, exit
    //        exit(1);
    //
    //    }
    //
    //    printf("jdwp library is not loaded, very good\n");

    // the other method is to directly check the presence of the jdwp library
    // in the /proc/<pid>/maps file. Therefore, we read the file and, if we
    // find the "jdwp" substring, we exit

    // get the pid of the process to get the right maps file
    int pid_integer = getpid();

    // convert the pid from integer into a string
    int pid_length = (int)((ceil(log10(pid_integer))+1)*sizeof(char));
    char pid_string[pid_length];
    sprintf(pid_string, "%i", pid_integer);

    // here we concatenate the pid with "/proc/" and "/maps" to create the path of the file to read
    char * file_path = (char *) malloc(strlen("/proc/") + strlen(pid_string) + strlen("/maps") + 1);
    strcpy(file_path, "/proc/");
    strcat(file_path, pid_string);
    strcat(file_path, "/maps");

    // open the maps file and check everything was ok
    FILE *maps_file  = fopen(file_path, "r");
    if (maps_file == NULL) {
        printf("Error! Could not open file\n");
        exit(-1);
    }

    // now read the file line by line. If we find the "jdwp" substring, it means
    // that we are debugged, therefore we exit
    char * line = NULL;
    size_t len = 0;
    ssize_t read;

    while ((read = getline(&line, &len, maps_file)) != -1) {
        if(strstr(line, "jdwp") != NULL) {
            printf("jdwp library is loaded, debugging is going on. Exiting...\n");
            exit(1);
        }
    }
    printf("jdwp library is not loaded, very good\n");
    fclose(maps_file);

    // if we are at the end, it means that there is no debugger.
    // therefore, just return
    return;
}

// This method checks whether the library jdwp.so, needed for java debugging, is
// loaded in memory or not. If it is so, the function overwrites ALL the library code
// segment loaded in the memory with 0xC3 (return for x86) opcode
JNIEXPORT void JNICALL Java_Utility_complexProtection(JNIEnv *env, jobject obj) {

    // check the presence of the jdwp library
    // in the /proc/<pid>/maps file. Therefore,
    // we read the file and, if we
    // find the "jdwp" substring, we modify the memory pages
    // by removing read access

    // get the pid of the process to get the right maps file
    int pid_integer = getpid();

    // convert the pid from integer into a string
    int pid_length = (int)((ceil(log10(pid_integer))+1)*sizeof(char));
    char pid_string[pid_length];
    sprintf(pid_string, "%i", pid_integer);

    // here we concatenate the pid with "/proc/" and "/maps" to create the path of the file to read
    char * file_path = (char *) malloc(strlen("/proc/") + strlen(pid_string) + strlen("/maps") + 1);
    strcpy(file_path, "/proc/");
    strcat(file_path, pid_string);
    strcat(file_path, "/maps");

    // open the maps file and check everything was ok
    FILE *maps_file  = fopen(file_path, "r");
    if (maps_file == NULL) {
        printf("Error! Could not open file\n");
        exit(-1);
    }

    // now read the file line by line. If we find the "jdwp" substring, it means
    // that we are debugged. In that case, we modify the memory page permission
    char * line = NULL;
    size_t len = 0;
    ssize_t read;

    // for each entry in the maps file
    while ((read = getline(&line, &len, maps_file)) != -1) {

        // if the entry is related to the jdwp library
        if(strstr(line, "jdwp") != NULL) {

            printf("%s", line);

            // by parsing the line, we retrieve the starting and ending addresses and permissions of the memory page
            char * temp;

            temp = strtok(line, "-");
            char starting_address_hex_char[strlen(temp) + 1];
            strncpy(starting_address_hex_char, temp, strlen(temp) + 1);

            temp = strtok(NULL, " ");
            char ending_address_hex_char[strlen(temp) + 1];
            strncpy(ending_address_hex_char, temp, strlen(temp) + 1);

            temp = strtok(NULL, " ");
            char permission_read  = temp[0];
            char permission_write = temp[1];
            char permission_exec  = temp[2];

            printf("start address %s and end address %s (HEX)\n", starting_address_hex_char, ending_address_hex_char);
            printf("permissions are %c, %c, %c\n", permission_read, permission_write, permission_exec);



            /*
            In the /proc/{pid}/maps file, there are four records with different permissions for JDWP
            (https://unix.stackexchange.com/questions/226283/shared-library-mappings-in-proc-pid-maps)
                - The r-xp entry describes a block of executable memory (x permission flag). That's the code.
                - The r--p entry describes a block of memory that is only readable (r permission flag). That's static data (constants).
                - The rw-p entry describes a block of memory that is writable (w permission flag). This is for global variables of the library.
                - The ---p entry describes a chunk of address space that doesn't have any permissions (or any memory mapped to it).

            by tampering the global variables   => SIGSEGV at frame libjdwp.so+0x15f92
            by tampering the static data        => SIGSEGV at frame ld-linux-x86-64.so.2+0xa72f
            by tampering the code               => if no breakpoint was set before the protection in the binary code was invoked
                                                        => every other breakpoint is ignored and the execution ends successfully
                                                => else
                                                        => SIGSEGV, but the frame is not reported
            */

            // Note: I empirically checked that the JDWP page that does not have any permission
            // cannot be tampered for more than 4096 bytes, otherwise I get a SIGBUS
            if (permission_exec == 'x') {

                // parse the addresses in base 10
                long starting_address_number    = strtol(starting_address_hex_char, NULL, 16);
                long ending_address_number      = strtol(ending_address_hex_char, NULL, 16);
                long difference_address_number  = ending_address_number - starting_address_number;
                char * starting_address         = (char *) starting_address_number;
                char * ending_address           = (char *) ending_address_number;
                printf("start address %p and end address %p \n", starting_address, ending_address);

                // change the flags to be able to write on the memory page
                if (mprotect(starting_address, difference_address_number, PROT_READ|PROT_WRITE|PROT_EXEC) == -1)
                    printf("Value of errno: %d (%s)\n", errno, strerror(errno));

                char * loop_address = starting_address;
                long i;

                for (i = 0; i < difference_address_number; i = i + 1l) {

                    *loop_address = 195;
                    ++loop_address;
                }

                // restore the permission flags of the memory address range
                int permissionToRestore = 0;
                if (permission_read  == 'r')
                    permissionToRestore = permissionToRestore + 1;
                if (permission_write == 'w')
                    permissionToRestore = permissionToRestore + 2;
                if (permission_exec  == 'x')
                    permissionToRestore = permissionToRestore + 4;

                printf("permissions to restore are %i\n", permissionToRestore);

                if (mprotect(starting_address, difference_address_number, permissionToRestore) == -1)
                    printf("Value of errno: %d (%s)\n", errno, strerror(errno));
            }
            else {

                printf("we skip this portion of memory addresses, I get a SIGBUS\n");
            }

            printf("\n");
        }
    }

    fclose(maps_file);
    return;
}




// =============== ANTI-DEBUGGING PROTECTION AGAINST JAVA-LEVEL DEBUGGER ===============
// This method checks whether the "jdwp.so" library, needed for java debugging, has been
// loaded. If so, this method overwrites some of the library code segments with the 0xC3
// opcode, that corresponds to "return" in x86 ISA. In particular, this method sets 0XC3
// at the beginning of each function in the "jdwp.so" library. In this way, the debugger
// returns without executing the function (e.g., set breakpoint) and without breaking it
// Indeed we want our Java Anti-Debuggging protection to operate as stealthy as possible
// Note: how to know when a function begins in the code segment of the "jdwp.so" library
// without having all the addresses? Well, usually each function starts with "push rbp".
JNIEXPORT void JNICALL Java_Utility_evenMoreComplexProtection (JNIEnv *env, jobject obj) {

    // Flag to understand whether we
    // applied the protection or not
    int wasTheProtectionApplied = 0;

    // Get the pid of the process to get
    // the related /proc/<pid>/maps file
    int pid_integer = getpid();

    // Convert the pid from integer to string
    int pid_length = (int)((ceil(log10(pid_integer))+1)*sizeof(char));
    char pid_string[pid_length];
    sprintf(pid_string, "%i", pid_integer);

    // Here we concatenate the pid with "/proc/" and
    // "/maps" to create the path of the file to read
    char * file_path = (char *) malloc(strlen("/proc/") + strlen(pid_string) + strlen("/maps") + 1);
    strcpy(file_path, "/proc/");
    strcat(file_path, pid_string);
    strcat(file_path, "/maps");

    // Check that we are able to open the maps file
    FILE *maps_file  = fopen(file_path, "r");
    if (maps_file == NULL) {
        // log
        // printf("Error while opening the maps file: %s", file_path);
        exit(-1);
    }


    char * line = NULL;
    size_t len = 0;
    ssize_t read;

    // For each entry in the maps file, if we
    // find the "jdwp" string, we are debugged!
    while ((read = getline(&line, &len, maps_file)) != -1) {

        if(strstr(line, "jdwp") != NULL) {

            // log
            // printf("%s", line);

            // By parsing the line, we retrieve the starting and
            // ending addresses and permissions of the memory page
            char * temp;

            temp = strtok(line, "-");
            char starting_address_hex_char[strlen(temp) + 1];
            strncpy(starting_address_hex_char, temp, strlen(temp) + 1);

            temp = strtok(NULL, " ");
            char ending_address_hex_char[strlen(temp) + 1];
            strncpy(ending_address_hex_char, temp, strlen(temp) + 1);

            temp = strtok(NULL, " ");
            char permission_read  = temp[0];
            char permission_write = temp[1];
            char permission_exec  = temp[2];

            // log
            // printf("start address %s and end address %s (HEX)\n", starting_address_hex_char, ending_address_hex_char);
            // printf("permissions are %c, %c, %c\n", permission_read, permission_write, permission_exec);

            // We are interested in tampering only the memory page with the
            // 'x' permission, i.e., the memory page with executable code.
            // Indeed, we want our AD protection to be as stealthy as possible
            if (permission_exec == 'x') {

                // parse the starting and ending addresses and permissions of the memory page to base 10
                long starting_address_number    = strtol(starting_address_hex_char, NULL, 16);
                long ending_address_number      = strtol(ending_address_hex_char, NULL, 16);
                long difference_address_number  = ending_address_number - starting_address_number;
                char * starting_address         = (char *) starting_address_number;
                char * ending_address           = (char *) ending_address_number;

                // log
                // printf("start address %p and end address %p \n", starting_address, ending_address);

                // Change the flags to be able to write on the memory page to overwrite the code of the debugger.
                // If there were errors in modifying the flags, something is off
                if (mprotect(starting_address, difference_address_number, PROT_READ|PROT_WRITE|PROT_EXEC) == -1) {
                    // log
                    // printf("Value of errno: %d (%s)\n", errno, strerror(errno));
                    exit(-1);
                }


                long i;
                uint8_t * loop_address = (uint8_t *) starting_address;

                // For each byte in the memory page, check whether the byte is the beginning of
                // a function (i.e., the opcode is 0x55 in hex, 85 in dec => 'push rbp'). If so,
                // then overwrite it with the return opcode (0XC3 in hex, 195 in dec)
                for (i = 0; i < difference_address_number; i = i + 1l) {
                     uint8_t byte8 = * loop_address;
                     if (byte8 == 85) {
                        *loop_address = 195;
                        wasTheProtectionApplied = 1;
                     }
                     loop_address++;
                }


                // Restore the permission flags of the memory page
                // 5 is '101', that maps in r-x (so read and execute but not write)
                // If there were errors in modifying the flags, something is off
                if (mprotect(starting_address, difference_address_number, 5) == -1) {
                    // log
                    // printf("Value of errno: %d (%s)\n", errno, strerror(errno));
                    exit(-1);
                }
            }

            // This condition is true if we did NOT apply the protection
            if (wasTheProtectionApplied == 0) {
                // log
                // printf("Protection was not applied");
                exit(-1);
            }
        }
    }

    // close the maps file and return
    fclose(maps_file);
    return;
}
