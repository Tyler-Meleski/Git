//Justin Lamberson, Tyler Meleski, Khalid Ibrahim
//Homework 2
//Date: 4/17/24

/*
Description: 
 */

import java.io.*;
import java.util.*;

public class HYPO {
    //Declaration of Global Variables
    static long MAR, MBR, IR, PSR, clock, SP, PC;
    static long[] RAM, GPR;
    static long opAddress;
    static long opValue;
    static long ProcessID = 1;
    static long WQ = -1;
    static long RQ;
    static long Waiting;
    static long OSFreeList;
    static long UserFreeList;


    //Declaration of error codes
    static final long OK = 0;
    static final long STATUS_HALT = 404;
    static final long ERROR_FILE_OPEN = -404;
    static final long ERROR_INVALID_ADDRESS = -405;
    static final long ERROR_NO_END_OF_PROGRAM = -1;
    static final long ERROR_INVALID_PC_VALUE = -2;
    static final long ERROR_OP_MODE_VALUE = -3;
    static final long ERROR_GPR_VALUE = -4;
    static final long ERROR_OPERAND_FETCH_FAILED = -5;
    static final long ERROR_DESTINATION_IMMEDIATE_MODE = -6;
    static final long ERROR_FATAL = -100;
    static final long ERROR_RUNTIME = -7;
    static final long ERROR_STACK_OVERFLOW = -8;
    static final long ERROR_STACK_UNDERFLOW = -9;
    static final long ERROR_NO_FREE_MEMORY = -1;
    static final long ERROR_INVALID_MEMORY_SIZE = -2;
    static final long ERROR_INVALID_MEMORY_ADDRESS = -3;
    //Declarations
    static final long PCB_SIZE = 22;
    static final long DefaultPriority = 128;
    static final long ReadyState = 1;
    static long EndOfList = -1;
    static final long MaxMemoryAddress = 9999;
    static final long UserMode = 2;
    static final long OSMode = 1;
    static final long stackSize = 20;
    static final long Timeslice = 200;
    static final long StartOfInputOperation = 123;
    static final long StartOfOutputOperation = 5619;

    /*
    //Written by: Justin Lamberson, Tyler Meleski
    // Function: Main
    //
    // Task Description:
    //  ...
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  OK - On successful execution
     */

    public static void main(String[] args) throws Exception {
        System.out.println("System initialized\n");
        InitializeSystem();

        boolean fileLoaded = true;
        Scanner reader = new Scanner(System.in);
        long returnValue;
        long executionCompletionStatus;

        boolean firstCycle = false;

        //FetchOperand(0, 3);

        //DumpMemory("Test message", 20000, 5); //Test to make sure that DumpMemory throws the proper exception

        //DumpMemory("Second Test", 500, 127);
        /*while(fileLoaded){
            System.out.println("Please insert name of file for execution:");
            returnValue = AbsoluteLoader(reader.nextLine());
            //System.out.println(returnValue);
            if(returnValue >= 0 && returnValue <= 9999){
                fileLoaded = false;
                PC = returnValue;
            }
        }

        DumpMemory("After Loading Program", 0, 99);
        executionCompletionStatus = CPUExecuteProgram();
        DumpMemory("After program execution", 0, 99);*/

        //return(executionCompletionStatus); //main method cannot have a return type

        boolean notShutodwn = true;

        long status;

        long processInterruptStatus;

        while(notShutodwn){
            //Check and process interrupt
            processInterruptStatus = CheckAndProcessInterrupt();
            //System.out.println("Infinite test");
            if(processInterruptStatus == 2){
                notShutodwn = false;
                break;
            }

            //Dump RQ and WQ
            System.out.println("RQ: Before CPU scheduling");
            if(!firstCycle){
                PrintQueue(RQ);
            }
            System.out.println("WQ: Before CPU scheduling");
            if(!firstCycle){
                PrintQueue(WQ); //TODO: Reinstate printqueue after fix
            }

            DumpMemory("Dynamic Memory Area before CPU scheduling\n", 0, 2999);

            //Select next process from RQ to give to CPU
            long Runningptr = SelectProcessFromRQ();

            //Preform restore context using dispatcher
            Dispatcher(Runningptr);

            System.out.println("RQ: After selecting process from RQ");
            //PrintQueue(RQ);

            //Dump Running PCB and CPU Context passing  Running PCB ptr as argument
            PrintPCB(Runningptr);

            //Execute instructions of the running process using the CPU
            status = CPUExecuteProgram();

            //Dump Dynamic Memory area
            DumpMemory("After execute program\n", 0, 2999);

            //Check return status - reason for giving up CPU
            if(status == 0){
                SaveContext(Runningptr); //Running process is losing cpu
                InsertIntoRQ(Runningptr);
                EndOfList = Runningptr;
            } else if(status == 404 || status < 0){ //Halt or run-time error
                TerminateProcess(Runningptr);
                EndOfList = Runningptr;
            } else if(status == StartOfInputOperation){ //io_getc
                RAM[(int) Runningptr + 3] = StartOfInputOperation;
                InsertIntoWQ(Runningptr);
                EndOfList = Runningptr;
            } else if(status == StartOfOutputOperation){ //io-putc
                RAM[(int) Runningptr + 3] = StartOfOutputOperation;
                InsertIntoWQ(Runningptr);
                EndOfList = Runningptr;
            }

            //1firstCycle = false;

        } //End of while not shutdown loop 


        System.out.println("Operating system is shutting down");

        //return status;  //Terminate Operating System //Main function cannot have a return status
    } //End of Main function

    /*
    // Function: InitializeSystem
    //
    // Task Description:
    //  Set all global system hardware components to 0
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  None
    */
    static void InitializeSystem(){
        RAM = new long[10000];
        MAR = 0;
        MBR = 0;
        IR = 0;
        GPR = new long[8];
        PSR = 0;
        clock = 0;
        SP = 0;
        PC = 0;
        RQ = EndOfList;     
        UserFreeList = 3000;
        RAM[3000] = EndOfList;
        RAM[3001] = 3000; //size of lock given in class
        OSFreeList = 6000;
        RAM[6000] = EndOfList;
        RAM[6001] = 4000; //Size of block given in class

        System.out.println(RAM[6000]);
        CreateProcess("nullProcess.txt", 0);

        return;
    }

    /*
    // Function: AbsoluteLoader
    //
    // Task Description:
    //      Open the file containing HYPO machine user program
    //      and load the content into memory
    //      On successful load, return the PC value in the End of Program line
    //      On failure, display appropriate error message and return appropriate error type
    //
    // Input Parameters
    //      filename        Name of the Hypo machine executable file
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  ErrorFileOpen           Unable to open the file
    //  ErrorInvalidAddress     Invalid address error
    //  ErrorNoEndOfProgram     Missing end of program indicator
    //  ErrorInvalidPCValue     invalid PC value
    //  0 to 9999               Successful Load, valid PC value
    */
    static long AbsoluteLoader(String filename){
        try{
            File fileImport = new File(filename);
            Scanner scan = new Scanner(fileImport);
            while(scan.hasNextLine()){
                String data = scan.nextLine();
                String[] imported = data.split(" ");
                int ramLocation = Integer.parseInt(imported[0]);
                long memoryData = Long.parseLong(imported[1]);
                if(ramLocation < -1 || ramLocation > 10000){
                    System.out.println("Error, invalid address");
                    scan.close();
                    return ERROR_INVALID_ADDRESS;
                } else if(ramLocation != -1 && (ramLocation > 0 || ramLocation < 10000)) {
                    RAM[ramLocation] = memoryData;
                } else if(ramLocation == -1 && (memoryData > 9999 || memoryData < 0)){
                    System.out.println("Error, invalid PC value");
                    scan.close();
                    return ERROR_INVALID_PC_VALUE;
                } else {
                    scan.close();
                    return Long.parseLong(imported[1]);
                }
            }
            scan.close();
        } catch (FileNotFoundException e) {
            System.out.println("Error, file not found");
            return ERROR_FILE_OPEN;
        }
        System.out.println("Error, no valid end of program");
        return ERROR_NO_END_OF_PROGRAM;
    }

    /*
    // Function: CPUexecuteProgram
    //
    // Task Description:
    //      Takes in instructions that are in memory and executes them
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //      OK                                  On successful execution
    //      ERROR_INVALID_PC_VALUE              Invalid PC value
    //      ERROR_GPR_VALUE                     Invalid GPR register
    //      ERROR_FETCH_OPERAND_VALUE           Invalid operand execution
    //      ERROR_DESTINATION_IMMEDIATE_MODE    Cannot assign values to set values
    //      ERROR_FATAL                         Fatal error, cannot divide by zero
    //      ERROR_RUNTIME                       PC outside expected range
    //      ERROR_STACK_OVERFLOW                SP over expected range
    //      ERROR_STACK_UNDERFLOW               SP under expected range
    //
    */
    static long CPUExecuteProgram(){
        long opCode;
        long remainder;
        long op1Mode;
        long op1GPR;
        long op2Mode;
        long op2GPR;
        long status;

        long op1Address;
        long op2Address;
        long op1Value;
        long op2Value;
        long result;
        long TimeLeft = Timeslice;

        boolean continueExecution = true;
        while(continueExecution && TimeLeft > 0){
            //System.out.print(TimeLeft);
            //Fetch Cycle
            // Fetch (read) the first word of the instruction pointed by PC into MBR
            // Instruction needing more words (2 word and 3 word instructions) are fetched
            // based on instruction (opcode)
            // when the operand 1 and operand 2 values are fetched using modes

            if ((0 <= PC) && (PC <= RAM.length)) {
                MAR = PC;
                PC++;
                MBR = RAM[(int) MAR];
            } else {
                System.out.println("Error, PC value outside expected range");
                return ERROR_INVALID_PC_VALUE;
            }

            IR = MBR;
            // Decode cycle
            // Decode the first word of the instruction into opcode,
            // operand 1 mode and operand 1 gpr and operand 2 mode and operand 2 gpr
            // using integer division and modulo operators
            // Five fields in the first word of any instruction is:
            // Opcode, Operand 1 mode, operand 1 GPR, Operand 2 mode, Operand 2 GPR

            opCode = IR / 10000;
            remainder = IR % 10000;

            op1Mode = remainder / 1000;
            remainder = remainder % 1000;

            op1GPR = remainder / 100;
            remainder = remainder % 100;

            op2Mode = remainder / 10;
            op2GPR = remainder % 10;


            if (op1GPR >= 8 || op1GPR < 0) {
                System.out.println("Error, operation 1 GPR out of bounds");
                return ERROR_GPR_VALUE;
            }
            if (op2GPR >= 8 || op2GPR < 0) {
                System.out.println("Error, operation 2 GPR out of bounds");
                return ERROR_GPR_VALUE;
            }

            /*System.out.println("OpCode " + opCode);
            System.out.println("Op1Mode " + op1Mode);
            System.out.println("Op1GPR " + op1GPR);
            System.out.println("Op2Mode " + op2Mode);
            System.out.println("Op2GPR " + op2GPR + "\n");*/

            switch ((int) opCode) {
                case 0: //HALT
                    System.out.println("Halt instruction encountered");
                    continueExecution = false;
                    clock += 12;
                    TimeLeft -= 12;
                    return STATUS_HALT;
                case 1: //ADD
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }
                    op1Value = opValue;
                    op1Address = opAddress;

                    status = fetchOperand(op2Mode, op2GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }

                    op2Value = opValue;
                    op2Address = opAddress;

                    result = op1Value + op2Value;

                    if (op1Mode == 1) { //Checks for register mode
                        GPR[(int) op1GPR] = result;
                    } else if (op1Mode == 6) { //Checks for immediate mode
                        //Error, destination cannot be immediate mode
                        System.out.println("Error, Destination operand cannot be immediate value");
                        return ERROR_DESTINATION_IMMEDIATE_MODE;
                    } else {
                        RAM[(int) op1Address] = result;
                    }
                    clock += 3;
                    TimeLeft -= 3;
                    break;
                case 2: //Subtract
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }
                    op1Value = opValue;
                    op1Address = opAddress;

                    status = fetchOperand(op2Mode, op2GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }

                    op2Value = opValue;
                    op2Address = opAddress;

                    result = op1Value - op2Value;

                    if (op1Mode == 1) { //Checks for register mode
                        GPR[(int) op1GPR] = result;
                    } else if (op1Mode == 6) { //Checks for immediate mode
                        //Error, destination cannot be immediate mode
                        System.out.println("Error, Destination operand cannot be immediate value");
                        return ERROR_DESTINATION_IMMEDIATE_MODE;
                    } else {
                        RAM[(int) op1Address] = result;
                    }
                    clock += 3;
                    TimeLeft -= 3;
                    break;
                case 3: //Multiply
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }
                    op1Value = opValue;
                    op1Address = opAddress;

                    status = fetchOperand(op2Mode, op2GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }

                    op2Value = opValue;
                    op2Address = opAddress;

                    result = op1Value * op2Value;

                    if (op1Mode == 1) { //Checks for register mode
                        GPR[(int) op1GPR] = result;
                    } else if (op1Mode == 6) { //Checks for immediate mode
                        //Error, destination cannot be immediate mode
                        System.out.println("Error, Destination operand cannot be immediate value");
                        return ERROR_DESTINATION_IMMEDIATE_MODE;
                    } else {
                        RAM[(int) op1Address] = result;
                    }
                    clock += 6;
                    TimeLeft -= 6;
                    break;
                case 4: //Divide
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }
                    op1Value = opValue;
                    op1Address = opAddress;

                    status = fetchOperand(op2Mode, op2GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }

                    op2Value = opValue;
                    op2Address = opAddress;

                    if (op2Value == 0) {
                        System.out.println("Fatal Error, cannot divide by zero");
                        return ERROR_FATAL;
                        //TODO: Check for fatal errors in main
                    }

                    result = op1Value / op2Value;

                    if (op1Mode == 1) { //Checks for register mode
                        GPR[(int) op1GPR] = result;
                    } else if (op1Mode == 6) { //Checks for immediate mode
                        //Error, destination cannot be immediate mode
                        System.out.println("Error, Destination operand cannot be immediate value");
                        return ERROR_DESTINATION_IMMEDIATE_MODE;
                    } else {
                        RAM[(int) op1Address] = result;
                    }
                    clock += 6;
                    TimeLeft -= 6;
                    break;
                case 5: //Move
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }
                    op1Value = opValue;
                    op1Address = opAddress;

                    status = fetchOperand(op2Mode, op2GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }

                    op2Value = opValue;
                    op2Address = opAddress;


                    if (op1Mode == 1) { //Checks for register mode
                        GPR[(int) op1GPR] = op2Value;
                    } else if (op1Mode == 6) { //Checks for immediate mode
                        //Error, destination cannot be immediate mode
                        System.out.println("Error, Destination operand cannot be immediate value");
                        return ERROR_DESTINATION_IMMEDIATE_MODE;
                    } else {
                        RAM[(int) op1Address] = op2Value;
                    }
                    clock += 2;
                    TimeLeft -= 2;
                    break;
                case 6: //Branch or jump on instruction
                    if (PC < 0 || PC > 9999) {
                        System.out.println("Error, runtime error");
                        return ERROR_RUNTIME;
                    } else {
                        PC = RAM[(int) PC];
                    }
                    clock += 2;
                    TimeLeft -= 2;
                    break;
                case 7: //Branch on minus
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }

                    op1Value = opValue;
                    op1Address = opAddress;

                    if (op1Value < 0) {
                        if (PC < 0 || PC > 9999) {
                            System.out.println("Error, runtime error");
                            return ERROR_RUNTIME;
                        } else {
                            PC = RAM[(int) PC];
                        }
                    } else {
                        PC++;
                    }
                    clock += 4;
                    TimeLeft -= 4;
                    break;
                case 8: //Branch on plus
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }

                    op1Value = opValue;
                    op1Address = opAddress;

                    if (op1Value > 0) {
                        if (PC < 0 || PC > 9999) {
                            System.out.println("Error, runtime error");
                            return ERROR_RUNTIME;
                        } else {
                            PC = RAM[(int) PC];
                        }
                    } else {
                        PC++;
                    }
                    clock += 4;
                    TimeLeft -= 4;
                    break;
                case 9: //Branch on zero
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }

                    op1Value = opValue;
                    op1Address = opAddress;

                    if (op1Value == 0) {
                        if (PC < 0 || PC > 9999) {
                            System.out.println("Error, runtime error");
                            return ERROR_RUNTIME;
                        } else {
                            PC = RAM[(int) PC];
                        }
                    } else {
                        PC++;
                    }
                    clock += 4;
                    TimeLeft -= 4;
                    break;
                case 10: //Push - if stack is not full
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }
                    op1Value = opValue;
                    op1Address = opAddress;

                    if (SP < 0 || SP > 9999) {
                        System.out.println("Error, stack overflow");
                        return ERROR_STACK_OVERFLOW;
                    }

                    SP++;
                    RAM[(int) SP] = op1Value;
                    clock += 2;
                    TimeLeft -= 2;
                    break;
                case 11: //Pop - if stack not empty
                    if (SP < 0 || SP > 9999) {
                        System.out.println("Error, stack underflow");
                        return ERROR_STACK_UNDERFLOW;
                    }
                    status = fetchOperand(op1Mode, op1GPR);
                    if (status != OK) {
                        System.out.println("Error, system cannot fetch operand");
                        return ERROR_OPERAND_FETCH_FAILED;
                    }
                    op1Value = opValue;
                    op1Address = opAddress;

                    RAM[(int) op1Address] = RAM[(int) SP];

                    SP--;
                    clock += 2;
                    TimeLeft -= 2;
                    break;
                case 12: //System call
                    status = fetchOperand(op1Mode, op1GPR);
                    if(status < 0) {
                        return status;
                    }
                    status = SystemCall(opValue);
                    clock += 12;
                    TimeLeft -= 12;
                    break;
                default:
                    System.out.println("Error, opCode outside expected parameters");
            }
        }
        return OK;
    }

    /*
    // Function: FetchOperand
    //
    // Task Description:
    //  ...
    //
    // Input Parameters
    //      OpMode      Operand mode value
    //      OpReg       Operand GPR value
    //
    // Output Parameters
    //      OpAddress   Address of operand
    //      OpValue     Operand value when mode and GPR are valid
    //
    // Function Return Value
    //      OK                      On successful fetch
    //      ErrorInvalidPC          PC value outside expected range
    //      ErrorInvalidAddress     Address outside expected range
    //      ErrorInvalidOpMode      OpMode outside expected range
    */
    static long fetchOperand(long opMode, long opReg){
        //Fetch operand based on the operand mode
        switch((int) opMode){
            case 1:
                opAddress = -1;
                opValue = GPR[(int) opReg];
                break;
            case 2:
                opAddress = GPR[(int) opReg];
                if(opAddress < 0 || opAddress >= 10000){
                    System.out.println("Error, address outside expected range");
                    return ERROR_INVALID_ADDRESS;
                } else {
                    opValue = RAM[(int) opAddress];
                }
                break;
            case 3:
                opAddress = GPR[(int) opReg];
                if(opAddress < 0 || opAddress >= 10000){
                    System.out.println("Error, address outside expected range");
                    return ERROR_INVALID_ADDRESS;
                } else {
                    opValue = RAM[(int) opAddress];
                }
                GPR[(int) opReg]++;
                break;
            case 4:
                GPR[(int) opReg]--;
                opAddress = GPR[(int) opReg];
                if(opAddress < 0 || opAddress >= 10000){
                    System.out.println("Error, address outside expected range");
                    return ERROR_INVALID_ADDRESS;
                } else {
                    opValue = RAM[(int) opAddress];
                }
                break;
            case 5:
                if(PC < 0 || PC > 9999){
                    System.out.println("Error, PC value outside expected range");
                    return ERROR_INVALID_PC_VALUE;
                }
                opAddress = RAM[(int) PC];
                PC++;
                if(opAddress < 0 || opAddress >= 10000){
                    System.out.println("Error, address outside expected range");
                    return ERROR_INVALID_ADDRESS;
                } else {
                    opValue = RAM[(int) opAddress];
                }
                break;
            case 6:
                if(PC < 0 || PC > 9999){
                    System.out.println("Error, PC value outside expected range");
                    return ERROR_INVALID_PC_VALUE;
                }
                opAddress = -1;
                opValue = RAM[(int) PC];
                PC++;
                break;
            default:
                System.out.println("Error, opMode outside expected parameters");
                return ERROR_OP_MODE_VALUE;
        }
        return OK;
    }

    /*
    // Function: DumpMemory
    //
    // Task Description:
    //      Displays a string passed as one of the input parameter
    //      Displays the content of GPRs, SP, PC, PSR, system clock, and
    //      the content of specified memory locations in a specific format
    //
    // Input Parameters
    //      Message         String to be displayed
    //      StartAddress    Start address of memory location
    //      Size            Number of locations to dump
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  None
    */
    static void DumpMemory(String message, long startAddress, long size) {
        long currentAddress = startAddress;
        long endAddress = startAddress + size;
        //Displays message
        System.out.println(message);

        //Statement to catch if user tries to dump outside expected memory
        if((startAddress + size) > RAM.length){
            System.out.println("Error. memory dump outside expected memory");
            return;
        }

        //Displays GPRs
        System.out.print("GPRs:\t\tG0\tG1\tG2\tG3\tG4\tG5\tG6\tG7\tSP\tPC\n\t\t"
                + GPR[0] + "\t" + GPR[1] + "\t" + GPR[2] + "\t" + GPR[3] + "\t" + GPR[4] + "\t" + GPR[5] + "\t" + GPR[6] + "\t" + GPR[7] + "\t" + SP + "\t" + PC + "\n");

        //Display memory addresses
        System.out.print("Address:\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\t+8\t+9\n");
        while(currentAddress < endAddress){
            System.out.print(currentAddress + "\t\t");
            for(int a = 0; a < 10; a++){
                if(currentAddress < endAddress){
                    System.out.print(RAM[(int) currentAddress] + "\t");
                    currentAddress++;
                } else {
                    break;
                }
            }
            System.out.println();

        }
        System.out.println("Clock: " + clock + "\nPSR: " + PSR);
    }
     /*
    // Written by: Tyler Meleski
    // Function: CreateProcess
    //
    // Task Description:
    // To take the filename and priority values as inputs and create a new process in the operating system
    //
    // Input Parameters
    // filename
    // priority
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  ERROR_INVALID_ADDRESS
    //  ptr
    //  OK - On successful execution
     */
    static long CreateProcess(String filename, long priority) {
        //Allocate space
        long PCBptr = AllocateOSMemory(PCB_SIZE);//sets PCBptr to allocateosmemeory passes pcb size

        if(PCBptr < 0) {
            System.out.print("Error, Memory Allocation failed");//Displays invalid address error message
            return ERROR_INVALID_ADDRESS;//Error code < 0
        }

        InitializePCB(PCBptr);//initialized pcb passing pcbptr

        long value = AbsoluteLoader(filename);//sets value by calling absoluteloader and passing filname

        if(value < 0 || value > MaxMemoryAddress) {
            System.out.print("Error, loading program");//Displays error loading program error message
            return ERROR_INVALID_ADDRESS;//error code < 0
        }
        //stores pc value in the pcb
        RAM[(int) (PCBptr + 20)] = value;

        long ptr = AllocateUserMemory(stackSize);//sets ptr to allocateusermem passes stacksize

        if(ptr < 0) {
            System.out.print("Error, User Memory Allocation Failed");//Displays user mem allocation failed error message
            FreeOSMemory(PCBptr, PCB_SIZE);//calls freeos function pass ptr and size
            return ptr;//returns error code
        }

        RAM[(int) (PCBptr + 19)] = ptr + stackSize;//empty stack is high address 
        RAM[(int) (PCBptr + 5)] = ptr;//sets stack address
        RAM[(int) (PCBptr + 6)] = stackSize;//sets stack size

        RAM[(int) (PCBptr + 4)] = priority;//sets priority

        DumpMemory("Program Area", 0, 200);//Dumps program area

        PrintPCB(PCBptr);//Prints PCB passes ptr
        InsertIntoRQ(PCBptr);//Insert pcb into rq passing the pointer

        return OK;
    }//End of CreateProcess function

    /*
    // Written by: Khalid Ibrahim
    // Function: InitializePCB
    //
    // Task Description:
    // To initialize the PCB to the correct values for execution
    //
    // Input Parameters
    //  PCBptr
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  Return
     */
    static void InitializePCB(long PCBptr) {
        //Array Initialization
        for(int i = 0; i < PCB_SIZE; i++) {
            RAM[(int) (PCBptr + i)] = 0;//Set PCB area to 0 using ptr
        }
        //Allocate PID and set it in the PCB
        RAM[(int) (PCBptr + 1)] = ProcessID++;//Set PID field in the PCB = ProessID
        RAM[(int) (PCBptr + 4)] = DefaultPriority;//Set priority field in the PCB = Default priority
        RAM[(int) (PCBptr + 3)] = ReadyState;//Set state field in the PCB = ReadyState
        RAM[(int) (PCBptr + 0)] = EndOfList;//Set Next PCBptr field in the PCB = EndOfList

        return;
    }//End of initializePCB function

    /*
    // Written By: Tyler Meleski
    // Function: PrintPCB
    //
    // Task Description:
    // To print the values from the PCB and the start address of the PCB
    //
    // Input Parameters
    // PCBptr
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // none
     */
    static void PrintPCB(long PCBptr) {
    /*Prints the values from the following fields Address, NextPCBptr, PID, State, PC,
    SP, Priority, stack Info start Address and 8 GPR Addresses*/
        System.out.print("PCB address = " + PCBptr + ", ");
    
        System.out.print("Next PCB Ptr = " + RAM[(int) (PCBptr + 0)] + ", ");
    
        System.out.print("PID = " + RAM[(int) (PCBptr + 1)] + ", ");
    
        System.out.print("State = " + RAM[(int) (PCBptr + 2)] + ", ");
    
        System.out.print("PC = " + RAM[(int) (PCBptr + 20)] + ", ");
    
        System.out.print("SP = " + RAM[(int) (PCBptr + 19)] + ", ");
    
        System.out.print("Priority = " + RAM[(int) (PCBptr + 4)] + ", ");
    
        System.out.print("Stack info: start address = " + RAM[(int) (PCBptr + 5)] + ", ");

        System.out.print("Size = " + RAM[(int) (PCBptr + 6)] + ", ");
        for(int i = 0; i < 8; i++) {
            System.out.print("GPR" + i + " = " + RAM[(int) (PCBptr + 11 + i)] + ", ");
        }
        System.out.println();
    }//End of PrintPCB function

    /*
    // Written by: Tyler Meleski
    // Function: PrintQueue
    //
    // Task Description:
    // To run through the queue and print each PCB value as you
    // pass through, the queue can be either ready or waiting
    //
    // Input Parameters
    // Qptr
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  OK - On successful execution
     */
    static long PrintQueue(long Qptr) {
        long currentPCBPtr = Qptr;//Sets currentPCBptr = Qptr

        if (currentPCBPtr == EndOfList  || currentPCBPtr == -1) {
            System.out.println("Empty list");//Displays empty list message
            return OK;
        }
        //Walks thru queue
        //Prints each PCB as we move on
        while(currentPCBPtr != EndOfList) {
            //System.out.println("ERROR\n");
            PrintPCB(currentPCBPtr);//Calls PrintPCB function pass current ptr
            currentPCBPtr = RAM[(int) currentPCBPtr];
        }//end of while loop
        return OK;
    }//End of PrintQueue function
    
    //Written By: Khalid Ibrahim
    //Function:
    /*InsertIntoRQ
    //
    //
    // Task Description:
    // To insert the PCB based on its priortiy according the CPU algorithm
    //
    // Input Parameters
    // PCBptr
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  OK - On successful execution
     */
    static long InsertIntoRQ(long PCBptr) {
        //Insert PCB accordingly to priority
        //Use priority in the PCB to find the correctplace to insert
        long previousPtr = EndOfList;//initialized to EOL
        long currentPtr = RQ;//Initialized to RQ

        //Checks for invalid PCB address
        if((PCBptr < 0) || (PCBptr > MaxMemoryAddress)) {
            System.out.print("Error, Invalid Address");//Displays invalid address error message
            return ERROR_INVALID_ADDRESS;//Error code < 0
        }
        RAM[(int) (PCBptr + 2)] = ReadyState;//sets state to ready state
        RAM[(int) (PCBptr + 0)] = EndOfList;//Setx next ptr to EOL
        //If the RQ is empty
        if(RQ == EndOfList) {
            RQ = PCBptr;
            return OK;
        }

        //Walks thru RQ to find place of insert
        //PCB is inserted at the end of priority
        while(currentPtr != EndOfList) {
            if(RAM[(int) (PCBptr + 4)] > RAM[(int) (currentPtr + 4)]) {
                //Place of insert is found
                if(previousPtr == EndOfList) {
                    //PCB is entered in front
                    RAM[(int) (PCBptr + 0)] = RQ;
                    RQ = PCBptr;
                    return OK;
                }
                //Enter PCB in the middle of list
                RAM[(int) (PCBptr + 0)] = RAM[(int) (previousPtr + 0)];
                RAM[(int) (previousPtr + 0)] = PCBptr;
                return OK;
            }
            else //PCB that is inserted has lower or equal priority to current PCB in RQ
            {
                previousPtr = currentPtr;
                currentPtr = RAM[(int) (currentPtr + 0)];
            }
        }//End of while loop
        RAM[(int) (previousPtr + 0)] = PCBptr;
        return OK;
    }//End of InsertIntoRQ function

    /*
    //Written by: Khalid Ibrahim
    // Function: InsertIntoWQ
    //
    // Task Description:
    // Inserts the given PCB into WQ at the front of the queue
    //
    // Input Parameters
    // PCBptr
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  OK - On successful execution
     */
    static long InsertIntoWQ(long PCBptr) {
        //if the PCBptr is in the invalid range
        if((PCBptr < 0) || (PCBptr > MaxMemoryAddress)) {
            System.out.print("Error, Invalid PCB Address");//Displays Invalid PCB address error message
            return ERROR_INVALID_ADDRESS;//Returns error code < 0
        }

        RAM[(int) (PCBptr + 2)] = Waiting;//Set state to ready state
        RAM[(int) (PCBptr + 0)] = WQ;//Sets next ptr to EOL

        WQ = PCBptr;

        return OK;
    }//end of InsertIntoWQ function

    /*
    // Written by: Tyler Meleski
    // Function: SelectProcessFromRQ
    //
    // Task Description:
    // To select the first process in the ready queue and return the ptr to the PCB
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  PCBptr
     */
    static long SelectProcessFromRQ() {
        long PCBptr = RQ;//Initialized ptr to RQ

        if(RQ != EndOfList) {
            //remove first PCB from RQ
            RQ = RAM[(int) (RQ + 0)];
        }
        //Set next point to EOL in the PCB
        RAM[(int) (PCBptr + 0)] = EndOfList;

        return PCBptr;
    }//End of SelectProcessFromRQ function

    /*
    // Written by: Tyler Meleski
    // Function: SaveContext
    //
    // Task Description:
    // To save the CPU context to help restore the PCB to the CPU later
    //
    // Input Parameters
    // PCBptr
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  
     */

    static void SaveContext(long PCBptr) {
        //Copy all CPU GPR's into the PCB using PCBptr
        //copy without using a loop
        RAM[(int) (PCBptr + 11)] = GPR[0];
        RAM[(int) (PCBptr + 12)] = GPR[1];
        RAM[(int) (PCBptr + 13)] = GPR[2];
        RAM[(int) (PCBptr + 14)] = GPR[3];
        RAM[(int) (PCBptr + 15)] = GPR[4];
        RAM[(int) (PCBptr + 16)] = GPR[5];
        RAM[(int) (PCBptr + 17)] = GPR[6];
        RAM[(int) (PCBptr + 18)] = GPR[7];

        RAM[(int) (PCBptr + 19)] = SP;//Save SP
        RAM[(int) (PCBptr + 20)] = PC;//Save PC

        return;
    }//End of SaveContext function

    /*
    // Written by: Tyler Meleski
    // Function: Dispatcher
    //
    // Task Description:
    // To help restore the context to help restore the context of the PCB into CPU
    //
    // Input Parameters
    // PCBptr
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  
     */
    static void Dispatcher(long PCBptr) {
        //Copy CPU GPR register values from given PCB into the CPU registers
        GPR[0] = RAM[(int) (PCBptr + 11)];
        GPR[1] = RAM[(int) (PCBptr + 12)];
        GPR[2] = RAM[(int) (PCBptr + 13)];
        GPR[3] = RAM[(int) (PCBptr + 14)];
        GPR[4] = RAM[(int) (PCBptr + 15)];
        GPR[5] = RAM[(int) (PCBptr + 16)];
        GPR[6] = RAM[(int) (PCBptr + 17)];
        GPR[7] = RAM[(int) (PCBptr + 18)];

        //Restore SP and PC from PCB
        SP = RAM[(int) (PCBptr + 19)];
        PC = RAM[(int) (PCBptr + 20)];

    
        PSR = UserMode;//UserMode is 2, OSMode is 1.

        return;
    }//End of Dispatcher function

    /*
    // Written by: Khaid Ibrahim
    // Function: Terminate Process
    //
    // Task Description:
    // This function is able to return 
    //
    // Input Parameters
    // PCBptr
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  
     */
    static void TerminateProcess(long PCBptr) {
        long stackStartAddress = RAM[(int) (PCBptr + 5)];
        long stackSize = RAM[(int) (PCBptr + 6)];

        //Returns stack memory using the address and size
        for (long i = stackStartAddress; i < stackStartAddress + stackSize; i++) {
            RAM[(int) i] = 0; 
        }
        //Returns PCB memory using ptr
        for (int i = 0; i < PCB_SIZE; i++) {
            RAM[(int) (PCBptr + i)] = 0;
        }
        return;
    }//End of TerminateProcess function

    /*
    // Written by: Tyler Meleski
    // Function: AllocateOSMemory
    //
    // Task Description:
    // To allocate the memory from OSFreeList and return its memory Addresses
    //
    // Input Parameters
    // RequestedSize
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  ERROR_NO_FREE_MEMORY            no free memory error message
    //  ERROR_INVALID_MEMORY_SIZE       invalid memory size error message
    //  CurrentPtr
     */
    static long AllocateOSMemory(long RequestedSize) {
        //Allocate mem from os
        if(OSFreeList == EndOfList) {
            System.out.print("Error, no free os memory");//Displays No free os error message
            return ERROR_NO_FREE_MEMORY;//Error code < 0
        }
        if(RequestedSize < 0) {
            System.out.print("Error, invalid size");//Displays invalid size error message
            return ERROR_INVALID_MEMORY_SIZE;//Error code < 0
        }
        if(RequestedSize == 1) {
            RequestedSize = 2;//The minimum size is 2 mem locations
        }

        long CurrentPtr = OSFreeList;//Initialized to OSFreeList
        long PreviousPtr = EndOfList;//Initialized to EOL

        while(CurrentPtr != EndOfList) {
            //Checks each block in the list
            if((RAM[(int) (CurrentPtr + 1)] == RequestedSize)) {
                //if block is found, ptrs will be adjusted
                if(CurrentPtr == OSFreeList)//if first block
                {
                    OSFreeList = RAM[(int) CurrentPtr];//first ptr entry
                    RAM[(int) CurrentPtr] = EndOfList;//Resets ptr
                    return CurrentPtr;//returns memory
                }
                else//if not first block
                {
                    RAM[(int) PreviousPtr] = RAM[(int) CurrentPtr];//next block
                    RAM[(int) CurrentPtr] = EndOfList;//reset next ptr
                    return CurrentPtr;//returns memory
                }
            }
            else if((RAM[(int) (CurrentPtr + 1)]) > RequestedSize)//if blocks size is greater than requestedsize
            {
                if(CurrentPtr == OSFreeList)//first block
                {
                    RAM[(int) (CurrentPtr + RequestedSize)] = RAM[(int) CurrentPtr];//move next block
                    RAM[(int) (CurrentPtr + RequestedSize + 1)] = RAM[(int) (CurrentPtr + 1)] - RequestedSize;
                    OSFreeList = CurrentPtr + RequestedSize;//reduced block address
                    RAM[(int) CurrentPtr] = EndOfList;//resets next ptr
                    return CurrentPtr;//returns memory
                }
                else//not first
                {
                    RAM[(int) (CurrentPtr + RequestedSize)] = RAM[(int) CurrentPtr];//move next block
                    RAM[(int) (CurrentPtr + RequestedSize + 1)] = RAM[(int) (CurrentPtr + 1)] - RequestedSize;
                    RAM[(int) PreviousPtr] = CurrentPtr + RequestedSize;//reduced block address
                    RAM[(int) CurrentPtr] = EndOfList;//resets next ptr
                    return CurrentPtr;//returns memory
                }
            }
            else//small block
            {
                //look to the next block
                PreviousPtr = CurrentPtr;
                CurrentPtr = RAM[(int) CurrentPtr];
            }
        }//End of while loop

        System.out.print("Error, no free os memory");//displays no free os error message
        return ERROR_NO_FREE_MEMORY;//error code < 0
    }//End of AllocateOSMemory function

    /*
    // Written by: Khalid Ibrahim
    // Function: FreeOSMemory
    //
    // Task Description:
    // To return the memory to os free space
    //
    // Input Parameters
    // ptr
    // size
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // ERROR_INVALID_MEMORY_ADDRESS     invalid memory address error message
    // OK                               on succesful executiom
     */
    static long FreeOSMemory(long ptr, long size) {
        if(ptr > 6000 || ptr < 9999) {//Address range given in class
            System.out.print("Error, Invalid Memory Address");//Displays Invalid Memory Address erorr message
            return ERROR_INVALID_MEMORY_ADDRESS;//error code < 0
        }
        if(size == 1) {
            size = 2;//2 is the minimum size
        }
        else if((size < 1 || (ptr + size) >= MaxMemoryAddress)) {
            //invalid size
            System.out.print("Error, Invalid Size or Memory Address");//displays invalid size or memory address error message
            return ERROR_INVALID_MEMORY_ADDRESS;//error code < 0
        }

        //Insert free block at beggining of OSfreelist
        RAM[(int) ptr] = OSFreeList;//free block pointed by os
        RAM[(int) (ptr + 1)] = size;//sets free block size
        OSFreeList = ptr;//sets os free list to block

        return OK;
    }//End of FreeOSMemory function

    /*
    // Written by: Tyler Meleski
    // Function: AllocateUserMemory
    //
    // Task Description:
    //  To Allocate the memory from UserFreeList and return the memory Addresses
    //
    // Input Parameters
    // Size
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // ERROR_NO_FREE_MEMORY         No free memory error message
    // ERROR_INVALID_MEMORY_SIZE    invalid memory siz error message
    // CurrentPtr
     */ 
    static long AllocateUserMemory(long size) {
    //use code from os and modified to userfreelist
        if(UserFreeList == EndOfList) {
            System.out.print("Error, no free user memory");//Displays no free user memory error message
            return ERROR_NO_FREE_MEMORY;//error code < 0
        }
        if(size < 0) {
            System.out.print("Error, invalid size");//Displays invalid size error message
            return ERROR_INVALID_MEMORY_SIZE;//error code < 0
        }
        if(size == 1) {
            size = 2;//minimum size is 2
        }

        long CurrentPtr = UserFreeList;//initialized to user freelist
        long PreviousPtr = EndOfList;//Initialized to EOL

        while(CurrentPtr != EndOfList) {
            //checks each block in the list
            if((RAM[(int) (CurrentPtr + 1)] == size)) {
                //if block is found ptrs are adjusted
                if(CurrentPtr == OSFreeList)//first block
                {
                    UserFreeList = RAM[(int) CurrentPtr];//first entry
                    RAM[(int) CurrentPtr] = EndOfList;//reset ptr to next block
                    return CurrentPtr;//return memory
                }
                else//not first
                {
                    RAM[(int) PreviousPtr] = RAM[(int) CurrentPtr];//points to next block
                    RAM[(int) CurrentPtr] = EndOfList;//resets ptr ti next block
                    return CurrentPtr;//returns memory
                }
            }
            else if((RAM[(int) (CurrentPtr + 1)]) > size)//found block has size greater than requested 
            {
                if(CurrentPtr == UserFreeList)//first block
                {
                    RAM[(int) (CurrentPtr + size)] = RAM[(int) CurrentPtr];//next block ptr
                    RAM[(int) (CurrentPtr + size + 1)] = RAM[(int) (CurrentPtr + 1)] - size;
                    UserFreeList = CurrentPtr + size;//reduced block adr
                    RAM[(int) CurrentPtr] = EndOfList;//resets ptr
                    return CurrentPtr;//returns memory
                }
                else//not first block
                {
                    RAM[(int) (CurrentPtr + size)] = RAM[(int) CurrentPtr];//next block ptr
                    RAM[(int) (CurrentPtr + size + 1)] = RAM[(int) (CurrentPtr + 1)] - size;
                    RAM[(int) PreviousPtr] = CurrentPtr + size;//reduced block adr
                    RAM[(int) CurrentPtr] = EndOfList;//restes ptr
                    return CurrentPtr;//returns memory
                }
            }
            else//small block
            {
                //looks to the next block
                PreviousPtr = CurrentPtr;
                CurrentPtr = RAM[(int) CurrentPtr];
            }
        }//end of while loop

        System.out.print("Error, no free os memory");//displays no free os memory error message
        return ERROR_NO_FREE_MEMORY;//error code < 0
    }//End of AllocateUserMemory function

    /*
    // Written by: Khlaid Ibrahim
    // Function: FreeUserMemory
    //
    // Task Description:
    //  To return the memory addresses form UserFreeList
    //
    // Input Parameters
    // ptr
    // size
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // ERROR_INVALID_MEMORY_ADDRESS     Invalid Memory Address error message
    // OK - On successful execution
     */
    static long FreeUserMemory(long ptr, long size) {
        //Return the memory to userfreespace
        if(ptr > 3000 || ptr < 5999) {//User memory area given in vlass
            System.out.print("Error, Invalid Memory Address");//Displays Invalid Address error
            return ERROR_INVALID_MEMORY_ADDRESS;//error code < 0
        }

        if(size == 1) {
            size = 2;//2 is the minimum size
        }
        else if((size < 1 || (ptr + size) > UserFreeList))//invalid size
        {
            System.out.print("Error, Invalid Size");//Displays Invalid Size error message
            return ERROR_INVALID_MEMORY_ADDRESS;//error code < 0
        }

        //Inserts free block at the beggining
        RAM[(int) ptr] = UserFreeList;
        RAM[(int) (ptr + 1)] = size;//sets block size
        UserFreeList = ptr;//sets userfreelist to block

        return OK;
    }//End of FreeUserMemory function

    /*
    // Written by: Tyler Meleski
    // Function: CheckAndProcessInterrupt
    //
    // Task Description:
    // To read the interrupt ID and service the interrupt depending on the value of the enetered ID
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // InterruptID          user input value of interruptID
     */ 
    static long CheckAndProcessInterrupt() {
        Scanner reader = new Scanner(System.in);
       
        //Displays the possible interrupts

        System.out.println("Interrupt ID's:"); 
        System.out.println("ID: 0 - No Interrupt");
        System.out.println("ID: 1 - Run program");
        System.out.println("ID: 2 - Shutdown System");
        System.out.println("ID: 3 - Input Operation Completion(io_getc)");
        System.out.println("ID: 4 - Output Operation Completion(io_putc");

        //Prompts user to enter the interrupt ID
        System.out.print("Enter Interrupt ID: ");
        long interruptID = reader.nextLong();
        System.out.println("\nRead Interrupt value: " + interruptID);//Reads and displays the interrupt ID

        switch((int) interruptID)
        {
        case 0://No Interrupt
            break;
        case 1://Run program
            ISRunProgramInterrupt();
            break;
        case 2://Shutdown System
            ISRshutdownSystem();
            break;
        case 3://Input operation completion - io_getc
            ISRinputCompletionInterrupt();
            break;
        case 4://Output operation completion - io_putc
            ISRoutputCompletionInterrupt();
            break;
        default://Invalid interruptID
            System.out.print("Error, Invalid Interrupt");
            break;
        }//End of switch

        return interruptID;
    }//End of CheckAndProcessInterrupt

    /*
    // Written By: Tyler Meleski
    // Function: ISRunProgramInterrupt
    //
    // Task Description:
    // To read the entered filename and to create the process with the passed filename
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  none
     */
    static void ISRunProgramInterrupt() {
        Scanner reader = new Scanner(System.in);
        //prompts anf reads the entered filename
        System.out.print("Enter filename: "); 
        String filename = reader.nextLine();

        CreateProcess(filename, DefaultPriority);//Callc create process function passes filename and defaultpriority
        return;
    }//End of ISRunProgram function

    /*
    // Khalid Ibrahim
    // Function: ISRinputCompletionInterrupt
    //
    // Task Description:
    //  Read the PID value and usingnio_getc to read one char from keyboard input
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  
     */
    static void ISRinputCompletionInterrupt() {
        Scanner reader = new Scanner(System.in);

        //Prompts and reads PID
        System.out.print("Enter PID: ");//Prompts
        long pid = reader.nextLong();//Reads Pid

        long PCB;//Delares PCB variable

        if(SearchAndRemovePCBfromWQ(pid) != EndOfList) {//If PCB is found its removed
            PCB = SearchAndRemovePCBfromWQ(pid);
            char character = reader.next().charAt(0);//Reads one char
            RAM[(int) (PCB + 11)] = (long) character;//Stores char in GPR
            RAM[(int) (PCB + 2)] = ReadyState;//sets process to readystate
            InsertIntoRQ(PCB);//inserts pcb into RQ
        }
        else if(RQ != EndOfList)//If theres no match
        {
            PCB = RQ;
            char character = reader.next().charAt(0);//reads one char from device keyboard
            RAM[(int) (PCB + 11)] = (long) character;//stores char in GPR
        }
        else//if no match in WQ and RQ
        {
            System.out.print("Error, Invalid Pid");//Display invalid pid error message
            return;
        }
    }//End of ISRinputCompletionInterrupt function

    /*
    // Written by: Khalid Ibrahim
    // Function: ISRoutputCompletionInterrupt
    //
    // Task Description:
    //  To read the pid and using the io_putc operation to display one char from the output device
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  
     */
    static void ISRoutputCompletionInterrupt() {
        Scanner reader = new Scanner(System.in);

        System.out.print("Enter PID: ");//Prompts
        long pid = reader.nextLong();//Reads Pid

        long PCB;//declared variable

        if(SearchAndRemovePCBfromWQ(pid) != EndOfList) {//If PCB is found its removed
            PCB = SearchAndRemovePCBfromWQ(pid);
            System.out.print((char) RAM[(int) (PCB + 11)]);//prints har in GPR
            RAM[(int) (PCB + 2)] = ReadyState;//Sets process state to readystate
            InsertIntoRQ(PCB);//inserts the pcb into RQ
        }
        else if(RQ == EndOfList) 
        {
            PCB = RQ;
            System.out.print((char) RAM[(int) (PCB + 11)]);//Prints the char in the GPR
        }
        else
        {
            System.out.print("Error, Invalid pid");//displays invalid pid error message
            return;
        }
    }//end of ISRoutputCompletionInterrupt function

    /*
    // Written by: Tyler Meleski
    // Function: ISRshutdownSystem
    //
    // Task Description:
    // Terminate all processes in the WQ and RQ and exit 
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  
     */
    static void ISRshutdownSystem() {
        //terminates processes in RQ
        long ptr = RQ;//ptr is pointed by RQ

        while(ptr != EndOfList) {
            RQ = RAM[(int) (ptr + 0)];//set to next ptr
            TerminateProcess(ptr);//Terminate function called and passes ptr
            ptr = RQ;
        }

        ptr = WQ;//ptr is pointed by WQ

        while(ptr != EndOfList) {
            WQ = RAM[(int) (ptr + 0)];//sets to next ptr
            TerminateProcess(ptr);//Terminate function called and passes ptr
            ptr = WQ;
        }

        return;
    }//end of ISRshutdownSystem function

    /*
    // Written by: Tyler Meleski
    // Function: SearchAndRemovePCBfromWQ
    //
    // Task Description:
    // To search WQ for a matched pid, if found the pid is removed from WQ
    //
    // Input Parameters
    // pid
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    //  CurrentPCBptr
    //  EndOfList
     */
    static long SearchAndRemovePCBfromWQ(long pid) {
        long currentPCBptr = WQ;
        long previousPCBptr = EndOfList;

        //Searches WQ for the pid
        while(currentPCBptr != EndOfList) 
        {
            //if a match is found its removed
            if(RAM[(int) (currentPCBptr + 1)] == pid) {
                if(previousPCBptr == EndOfList) 
                {
                    //first pid
                    WQ = RAM[(int) (currentPCBptr + 0)];
                }
                else 
                {
                    //not first
                    RAM[(int) (previousPCBptr + 0)] = RAM[(int) (currentPCBptr + 0)];
                }
                RAM[(int) (currentPCBptr + 0)] = EndOfList;
                return currentPCBptr;
            }
            previousPCBptr = currentPCBptr;
            currentPCBptr = RAM[(int) (currentPCBptr + 0)];
        }

        System.out.print("Error, display PID not found");//displays pid not found error message

        return EndOfList;
    }//End of SearchAndRemovePCBfromWQ function

    /*
    // Written by: Justin Lamberson, Tyler Meleski
    // Function: SystemCall
    //
    // Task Description:
    //  To run the correct process depending on the user entered value fo systemcallid
    //
    // Input Parameters
    // SystemCallID         input value of ID
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // status               status of the systemcall
     */
    static long SystemCall(long SystemCallID) {
        PSR = OSMode;//sets system to OS mode

        System.out.println("System call ID: " + SystemCallID);//displays systemcallID

        Long status = OK;//declares long status and sets to ok

        switch((int) SystemCallID)
        {
            case 1://Create Process
                System.out.print("Create Process System Call Not Implemented");
                break;
            case 2://Delete process
                System.out.print("Delete Process System Call Not Implemented");
                break;
            case 3://Process inquiry
                System.out.print("Process Inquiry System Call Not Implemented");
                break;
            case 4://Dynamic memory Allocation
                status = MemAllocSystemCall();
                break;
            case 5://Free Dynamic memory Allocation
                status = MemFreeSystemCall();
                break;
            case 8://io_getc
                status = io_getcSystemCall();
                break;
            case 9://io_putc
                status = io_putcSystemCall();
                break;
            default://Invalid system ID
                System.out.print("Error, Invalid System call ID");
                break;
        }

        PSR = UserMode;//sets system mode to usermode

        return status;//returns status
    }//End of SystemCallID function

    /*
    // Written by: Tyler Meleski
    // Function: MemAllocSystemCall
    //
    // Task Description:
    // To allocate memory from userfreelist
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // ERROR_INVALID_MEMORY_SIZE        Invalid memory size error message
    // GPR[0]                           First value of GPR
     */
    static long MemAllocSystemCall() {
        AllocateUserMemory(UserFreeList);

        long Size = GPR[2];//Declare long size and set it to GPR2 value

        if(Size < 1)//Checks for size out of range
        {
            System.out.print("Error, Invalid Size");//Displays Invalid size error message
            return ERROR_INVALID_MEMORY_SIZE;//error code < 0
        }

        if(Size == 1)//Checks for size 1 and changes to 2
        {
            Size = 2;
        }

        GPR[1] = AllocateUserMemory(Size);

        if(GPR[1] < 0) {
            GPR[0] = GPR[1];//sets GPR0 to return status
        }
        else 
        {
            GPR[0] = OK;//Sets GPR0 to ok
        }

        System.out.print(GPR[0] + GPR[1] + GPR[2]);

        return GPR[0];
    }//End of MemAllocaSystemCall function

    /*
    // Written by: Tyler Meleski
    // Function: MemFreeSystemCall
    //
    // Task Description:
    //  Returns the dynamically allocated user free memory to the user free list
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // GPR[0]       Value of the first GPR
     */
    static long MemFreeSystemCall() {
        AllocateUserMemory(UserFreeList);

        long Size = GPR[2];//Declares Long size and sets it GPR2 value

        if(Size < 1) {
            System.out.print("Error, Invalid Size");//Displays invalid size error message
            return ERROR_INVALID_MEMORY_SIZE;//error code < 0
        }

        if(Size == 1)//Check size 1 and change it to size 2
        {
            Size = 2;
        }

        GPR[0] = FreeUserMemory(GPR[1], Size);//calls free user memory function passes GPR 1 and size

        System.out.print(MemFreeSystemCall() + GPR[0] +", " + GPR[1] + ", " + GPR[2]);//Prints GPR 1 2 and 3
        return GPR[0];
    }//End of MEMFreeSystemCall
    
    /*
    // Written by: Khalid Ibrahim
    // Function: io_getcSystemCall
    //
    // Task Description:
    // To read one char from a standard input device keyboard
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // StartOfInputOperation 
     */
    static long io_getcSystemCall() {
        return StartOfInputOperation;
    }

    /*
    // Written by: Khalid Ibrahim
    // Function: io_putcSystemCall
    //
    // Task Description:
    //  To display one char of the standard output device
    //
    // Input Parameters
    //  None
    //
    // Output Parameters
    //  None
    //
    // Function Return Value
    // StartOfOutputOperation
     */
    static long io_putcSystemCall() {
        return StartOfOutputOperation;
    }
}
