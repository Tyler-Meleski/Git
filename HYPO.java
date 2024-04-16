//Tyler Meleski
//Homework 2
//Date:

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
    static long WQ;
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

        //FetchOperand(0, 3);

        //DumpMemory("Test message", 20000, 5); //Test to make sure that DumpMemory throws the proper exception

        //DumpMemory("Second Test", 500, 127);
        while(fileLoaded){
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
        DumpMemory("After program execution", 0, 99);

        //return(executionCompletionStatus); //main method cannot have a return type

        boolean notShutodwn = true;

        long status;

        while(notShutodwn){
            //Check and process interrupt
            long processInterruptStatus = CheckAndProcessInterrupt();
            System.out.println("Infinite test");
            if(processInterruptStatus == 2){
                notShutodwn = false;
                break;
            }

            //Dump RQ and WQ
            System.out.println("RQ: Before CPU scheduling");
            PrintQueue(RQ);
            System.out.println("WQ: Before CPU scheduling");
            PrintQueue(WQ);
            DumpMemory("Dynamic Memory Area before CPU scheduling\n", 0, 2999);

            //Select next process from RQ to give to CPU
            long Runningptr = SelectProcessFromRQ();

            //Preform restore context using dispatcher
            Dispatcher(Runningptr);

            System.out.println("RQ: After selecting process from RQ");
            PrintQueue(RQ);

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

        //CreateProcess(null, 0); //TODO: Reinstate later

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
                    break;
                case 6: //Branch or jump on instruction
                    if (PC < 0 || PC > 9999) {
                        System.out.println("Error, runtime error");
                        return ERROR_RUNTIME;
                    } else {
                        PC = RAM[(int) PC];
                    }
                    clock += 2;
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
    // Function: CreateProcess
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
    static long CreateProcess(String filename, long priority) {

        long PCBptr = AllocateOSMemory(PCB_SIZE);
        if(PCBptr < 0) {
            System.out.print("Error, Invalid Address");
            return ERROR_INVALID_ADDRESS;
        }

        InitializePCB(PCBptr);

        long value = AbsoluteLoader(filename);
        if(value < 0 || value > MaxMemoryAddress) {
            System.out.print("Error, loading program");
            return ERROR_INVALID_ADDRESS;
        }

        RAM[(int) (PCBptr + 20)] = value;

        long ptr = AllocateUserMemory(stackSize);

        if(ptr < 0) {
            System.out.print("Error, User Memeory Allocation Failed");
            FreeOSMemory(PCBptr, PCB_SIZE);
            return ptr;
        }

        RAM[(int) (PCBptr + 19)] = ptr + stackSize;
        RAM[(int) (PCBptr + 5)] = ptr;
        RAM[(int) (PCBptr + 6)] = stackSize;

        RAM[(int) (PCBptr + 4)] = priority;

        DumpMemory("Program Area", 0, 99);

        PrintPCB(PCBptr);
        InsertIntoRQ(PCBptr);

        return OK;
    }
    /*
    // Function: InitializePCB
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
    static void InitializePCB(long PCBptr) {
        for(int i = 0; i < PCB_SIZE; i++) {
            RAM[(int) (PCBptr + i)] = 0;
        }

        RAM[(int) (PCBptr + 1)] = ProcessID++;
        RAM[(int) (PCBptr + 4)] = DefaultPriority;
        RAM[(int) (PCBptr + 3)] = ReadyState;
        RAM[(int) (PCBptr + 0)] = EndOfList;

        return;
    }
    /*
    // Function: PrintPCB
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

    static void PrintPCB(long PCBptr) {
   
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
    }
    /*
    // Function: PrintQueue
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

    static long PrintQueue(long Qptr) {
        long currentPCBPtr = Qptr;

        if (currentPCBPtr == EndOfList) {
            System.out.println("Empty list");
            return OK;
        }

        while(currentPCBPtr != EndOfList) {
            PrintPCB(currentPCBPtr);
            currentPCBPtr = RAM[(int) currentPCBPtr];
        }
        return OK;
    }

    /*InsertIntoRQ
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
    static long InsertIntoRQ(long PCBptr) {
        long previousPtr = EndOfList;
        long currentPtr = RQ;

        if((PCBptr < 0) || (PCBptr > MaxMemoryAddress)) {
            System.out.print("Error, Invalid Address");
            return ERROR_INVALID_ADDRESS;
        }
        RAM[(int) (PCBptr + 2)] = ReadyState;
        RAM[(int) (PCBptr + 0)] = EndOfList;

        if(RQ == EndOfList) {
            RQ = PCBptr;
            return OK;
        }

        while(currentPtr != EndOfList) {
            if(RAM[(int) (PCBptr + 4)] > RAM[(int) (currentPtr + 4)]) {
                if(previousPtr == EndOfList) {
                    RAM[(int) (PCBptr + 0)] = RQ;
                    RQ = PCBptr;
                    return OK;
                }
                RAM[(int) (PCBptr + 0)] = RAM[(int) (previousPtr + 0)];
                RAM[(int) (previousPtr + 0)] = PCBptr;
                return OK;
            }
            else 
            {
                previousPtr = currentPtr;
                currentPtr = RAM[(int) (currentPtr + 0)];
            }
        }
        RAM[(int) (previousPtr + 0)] = PCBptr;
        return OK;
    }
    /*
    // Function: InsertIntoWQ
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

    static long InsertIntoWQ(long PCBptr) {
        if((PCBptr < 0) || (PCBptr > MaxMemoryAddress)) {
            System.out.print("Error, Invalid PCB Address");
            return ERROR_INVALID_ADDRESS;
        }

        RAM[(int) (PCBptr + 2)] = Waiting;
        RAM[(int) (PCBptr + 0)] = WQ;

        WQ = PCBptr;

        return OK;
    }
    /*
    // Function: SelectProcessFromRQ
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

    static long SelectProcessFromRQ() {
        long PCBptr = RQ;

        if(RQ != EndOfList) {
            RQ = RAM[(int) (RQ + 0)];
        }

        RAM[(int) (PCBptr + 0)] = EndOfList;

        return PCBptr;
    }

    /*
    // Function: SaveContext
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

    static void SaveContext(long PCBptr) {
    
        RAM[(int) (PCBptr + 11)] = GPR[0];
        RAM[(int) (PCBptr + 12)] = GPR[1];
        RAM[(int) (PCBptr + 13)] = GPR[2];
        RAM[(int) (PCBptr + 14)] = GPR[3];
        RAM[(int) (PCBptr + 15)] = GPR[4];
        RAM[(int) (PCBptr + 16)] = GPR[5];
        RAM[(int) (PCBptr + 17)] = GPR[6];
        RAM[(int) (PCBptr + 18)] = GPR[7];

    
        RAM[(int) (PCBptr + 19)] = SP;
        RAM[(int) (PCBptr + 20)] = PC;

        return;
    }

    /*
    // Function: Dispatcher
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

    static void Dispatcher(long PCBptr) {
        
        GPR[0] = RAM[(int) (PCBptr + 11)];
        GPR[1] = RAM[(int) (PCBptr + 12)];
        GPR[2] = RAM[(int) (PCBptr + 13)];
        GPR[3] = RAM[(int) (PCBptr + 14)];
        GPR[4] = RAM[(int) (PCBptr + 15)];
        GPR[5] = RAM[(int) (PCBptr + 16)];
        GPR[6] = RAM[(int) (PCBptr + 17)];
        GPR[7] = RAM[(int) (PCBptr + 18)];

        SP = RAM[(int) (PCBptr + 19)];
        PC = RAM[(int) (PCBptr + 20)];

    
        PSR = UserMode; // UserMode is 2, OSMode is 1.

        return;
    }
    /*
    // Function: Terminate Process
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
    static void TerminateProcess(long PCBptr) {
        long stackStartAddress = RAM[(int) (PCBptr + 5)];
        long stackSize = RAM[(int) (PCBptr + 6)];

        for (long i = stackStartAddress; i < stackStartAddress + stackSize; i++) {
            RAM[(int) i] = 0; 
        }

        for (int i = 0; i < PCB_SIZE; i++) {
            RAM[(int) (PCBptr + i)] = 0;
        }
        return;
    }
    /*
    // Function: AllocateOSMemory
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
    static long AllocateOSMemory(long RequestedSize) {

        if(OSFreeList == EndOfList) {
            System.out.print("Error, no free os memory");
            return ERROR_NO_FREE_MEMORY;
        }
        if(RequestedSize < 0) {
            System.out.print("Error, invalid size");
            return ERROR_INVALID_MEMORY_SIZE;
        }
        if(RequestedSize == 1) {
            RequestedSize = 2;
        }

        long CurrentPtr = OSFreeList;
        long PreviousPtr = EndOfList;

        while(CurrentPtr != EndOfList) {
            if((RAM[(int) (CurrentPtr + 1)] == RequestedSize)) {
                if(CurrentPtr == OSFreeList) {
                    OSFreeList = RAM[(int) CurrentPtr];
                    RAM[(int) CurrentPtr] = EndOfList;
                    return CurrentPtr;
                }
                else 
                {
                    RAM[(int) PreviousPtr] = RAM[(int) CurrentPtr];
                    RAM[(int) CurrentPtr] = EndOfList;
                    return CurrentPtr;
                }
            }
            else if((RAM[(int) (CurrentPtr + 1)]) > RequestedSize) {
                if(CurrentPtr == OSFreeList)
                {
                    RAM[(int) (CurrentPtr + RequestedSize)] = RAM[(int) CurrentPtr];
                    RAM[(int) (CurrentPtr + RequestedSize + 1)] = RAM[(int) (CurrentPtr + 1)] - RequestedSize;
                    OSFreeList = CurrentPtr + RequestedSize;
                    RAM[(int) CurrentPtr] = EndOfList;
                    return CurrentPtr;
                }
                else 
                {
                    RAM[(int) (CurrentPtr + RequestedSize)] = RAM[(int) CurrentPtr];
                    RAM[(int) (CurrentPtr + RequestedSize + 1)] = RAM[(int) (CurrentPtr + 1)] - RequestedSize;
                    RAM[(int) PreviousPtr] = CurrentPtr + RequestedSize;
                    RAM[(int) CurrentPtr] = EndOfList;
                    return CurrentPtr;
                }
            }
            else 
            {
                PreviousPtr = CurrentPtr;
                CurrentPtr = RAM[(int) CurrentPtr];
            }
        
        }
        System.out.print("Error, no free os memory");
        return ERROR_NO_FREE_MEMORY;
    }
    /*
    // Function: FreeOSMemory
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
    static long FreeOSMemory(long ptr, long size) {
        if(ptr > 6000 || ptr < 9999) {//Address range given in class
            System.out.print("Error, Invalid Memory Address");
            return ERROR_INVALID_MEMORY_ADDRESS;
        }
        if(size == 1) {
            size = 2;
        }
        else if((size < 1 || (ptr + size) >= MaxMemoryAddress)) {
            System.out.print("Error, Invalid Size or Memory Address");
            return ERROR_INVALID_MEMORY_ADDRESS;
        }

        RAM[(int) ptr] = OSFreeList;
        RAM[(int) (ptr + 1)] = size;
        OSFreeList = ptr;

        return OK;
    }
    /*
    // Function: AllocateUserMemory
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
   
    static long AllocateUserMemory(long size) {
        if(UserFreeList == EndOfList) {
            System.out.print("Error, no free os memory");
            return ERROR_NO_FREE_MEMORY;
        }
        if(size < 0) {
            System.out.print("Error, invalid size");
            return ERROR_INVALID_MEMORY_SIZE;
        }
        if(size == 1) {
            size = 2;
        }

        long CurrentPtr = UserFreeList;
        long PreviousPtr = EndOfList;

        while(CurrentPtr != EndOfList) {
            if((RAM[(int) (CurrentPtr + 1)] == size)) {
                if(CurrentPtr == OSFreeList) {
                    UserFreeList = RAM[(int) CurrentPtr];
                    RAM[(int) CurrentPtr] = EndOfList;
                    return CurrentPtr;
                }
                else 
                {
                    RAM[(int) PreviousPtr] = RAM[(int) CurrentPtr];
                    RAM[(int) CurrentPtr] = EndOfList;
                    return CurrentPtr;
                }
            }
            else if((RAM[(int) (CurrentPtr + 1)]) > size) {
                if(CurrentPtr == UserFreeList)
                {
                    RAM[(int) (CurrentPtr + size)] = RAM[(int) CurrentPtr];
                    RAM[(int) (CurrentPtr + size + 1)] = RAM[(int) (CurrentPtr + 1)] - size;
                    UserFreeList = CurrentPtr + size;
                    RAM[(int) CurrentPtr] = EndOfList;
                    return CurrentPtr;
                }
                else 
                {
                    RAM[(int) (CurrentPtr + size)] = RAM[(int) CurrentPtr];
                    RAM[(int) (CurrentPtr + size + 1)] = RAM[(int) (CurrentPtr + 1)] - size;
                    RAM[(int) PreviousPtr] = CurrentPtr + size;
                    RAM[(int) CurrentPtr] = EndOfList;
                    return CurrentPtr;
                }
            }
            else 
            {
                PreviousPtr = CurrentPtr;
                CurrentPtr = RAM[(int) CurrentPtr];
            }
        
        }
        System.out.print("Error, no free os memory");
        return ERROR_NO_FREE_MEMORY;
    }
    /*
    // Function: FreeUserMemory
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
    static long FreeUserMemory(long ptr, long size) {

        if(ptr > 3000 || ptr < 5999) {//User memory area given in vlass
            System.out.print("Error, Invalid Memory Address");
            return ERROR_INVALID_MEMORY_ADDRESS;
        }

        if(size == 1) {
            size = 2;
        }
        else if((size < 1 || (ptr + size) > UserFreeList)) {
            System.out.print("Error, Invalid Size");
            return ERROR_INVALID_MEMORY_ADDRESS;
        }

        RAM[(int) ptr] = UserFreeList;
        RAM[(int) (ptr + 1)] = size;
        UserFreeList = ptr;

        return OK;
    }
    /*
    // Function: CheckAndProcessInterrupt
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
    static long CheckAndProcessInterrupt() {
        Scanner reader = new Scanner(System.in);
       
        System.out.println("Enter Interrupt ID: ");
        System.out.println("Interrupt ID's:"); 
        System.out.println("ID: 0 - No Interrupt");
        System.out.println("ID: 1 - Run program");
        System.out.println("ID: 2 - Shutdown System");
        System.out.println("ID: 3 - Input Operation Completion(io_getc)");
        System.out.println("ID: 4 - Output Operation Completion(io_putc");

        long interruptID = reader.nextLong();
        System.out.println("Read Interrupt value: " + interruptID);

        switch((int) interruptID)
        {
        case 0:
            break;
        case 1:
            ISRunProgramInterrupt();
            break;
        case 2:
            ISRshutdownSystem();
            break;
        case 3:
            ISRinputCompletionInterrupt();
            break;
        case 4:
            ISRoutputCompletionInterrupt();
            break;
        default:
            System.out.print("Error, Invalid Interrupt");
            break;
        }

        return interruptID;
    }
    /*
    // Function: ISRunProgramInterrupt
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
    static void ISRunProgramInterrupt() {
        Scanner reader = new Scanner(System.in);
   
        System.out.print("Enter filename: "); 
        String filename = reader.nextLine();

        CreateProcess(filename, DefaultPriority);
        return;
    }
    /*
    // Function: ISRinputCompletionInterrupt
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
    static void ISRinputCompletionInterrupt() {
        Scanner reader = new Scanner(System.in);

        System.out.print("Enter PID: ");//Prompts
        long pid = reader.nextLong();//Reads Pid

        long PCB;

        if(SearchAndRemovePCBfromWQ(pid) != EndOfList) {//If PCB is found its removed
            PCB = SearchAndRemovePCBfromWQ(pid);
            char character = reader.next().charAt(0);//Reads one char
            RAM[(int) (PCB + 11)] = (long) character;//Stores char in GPR
            RAM[(int) (PCB + 2)] = ReadyState;
            InsertIntoRQ(PCB);
        }
        else if(RQ != EndOfList)
        {
            PCB = RQ;
            char character = reader.next().charAt(0);
            RAM[(int) (PCB + 11)] = (long) character;
        }
        else 
        {
            System.out.print("Error, Invalid Pid");
            return;
        }
    }
    /*
    // Function: ISRoutputCompletionInterrupt
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
    static void ISRoutputCompletionInterrupt() {
        Scanner reader = new Scanner(System.in);

        System.out.print("Enter PID: ");//Prompts
        long pid = reader.nextLong();//Reads Pid

        long PCB;

        if(SearchAndRemovePCBfromWQ(pid) != EndOfList) {//If PCB is found its removed
            PCB = SearchAndRemovePCBfromWQ(pid);
            System.out.print((char) RAM[(int) (PCB + 11)]);
            RAM[(int) (PCB + 2)] = ReadyState;
            InsertIntoRQ(PCB);
        }
        else if(RQ == EndOfList) 
        {
            PCB = RQ;
            System.out.print((char) RAM[(int) (PCB + 11)]);
        }
        else
        {
            System.out.print("Error, Invalid pid");
            return;
        }
    }
    /*
    // Function: ISRshutdownSystem
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
    static void ISRshutdownSystem() {

        long ptr = RQ;

        while(ptr != EndOfList) {
            RQ = RAM[(int) (ptr + 0)];
            TerminateProcess(ptr);
            ptr = RQ;
        }

        ptr = WQ;

        while(ptr != EndOfList) {
            WQ = RAM[(int) (ptr + 0)];
            TerminateProcess(ptr);
            ptr = WQ;
        }

        return;
    }
    /*
    // Function: SearchAndRemovePCBfromWQ
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
    static long SearchAndRemovePCBfromWQ(long pid) {
        long currentPCBptr = WQ;
        long previousPCBptr = EndOfList;

        while(currentPCBptr != EndOfList) {
            if(RAM[(int) (currentPCBptr + 1)] == pid) {
                if(previousPCBptr == EndOfList) {
                    WQ = RAM[(int) (currentPCBptr + 0)];
                }
                else 
                {
                    RAM[(int) (previousPCBptr + 0)] = RAM[(int) (currentPCBptr + 0)];
                }
                RAM[(int) (currentPCBptr + 0)] = EndOfList;
                return currentPCBptr;
            }
            previousPCBptr = currentPCBptr;
            currentPCBptr = RAM[(int) (currentPCBptr + 0)];
        }

        System.out.print("Error, display PID not found");

        return EndOfList;
    }
    /*
    // Function: SystemCall
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
    static long SystemCall(long SystemCallID) {
        PSR = OSMode;

        System.out.println("System call ID: " + SystemCallID);

        Long status = OK;

        switch((int) SystemCallID)
        {
            case 1:
                System.out.print("Create Process System Call Not Implemented");
                break;
            case 2:
                System.out.print("Delete Process System Call Not Implemented");
                break;
            case 3:
                System.out.print("Process Inquiry System Call Not Implemented");
                break;
            case 4:
                status = MemAllocSystemCall();
                break;
            case 5:
                status = MemFreeSystemCall();
                break;
            case 8:
                status = io_getcSystemCall();
                break;
            case 9:
                status = io_putcSystemCall();
                break;
            default:
                System.out.print("Error, Invalid System call ID");
                break;
        }

        PSR = UserMode;

        return status;
    }
    /*
    // Function: MemAllocSystemCall
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
    static long MemAllocSystemCall() {
        AllocateUserMemory(UserFreeList);

        long Size = GPR[2];

        if(Size < 1) {
            System.out.print("Error, Invalid Size");
            return ERROR_INVALID_MEMORY_SIZE;
        }

        if(Size == 1) {
            Size = 2;
        }

        GPR[1] = AllocateUserMemory(Size);

        if(GPR[1] < 0) {
            GPR[0] = GPR[1];
        }
        else 
        {
            GPR[0] = OK;
        }

        System.out.print(MemAllocSystemCall() + GPR[0] + GPR[1] + GPR[2]);

        return GPR[0];
    }
    /*
    // Function: MemFreeSystemCall
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
    static long MemFreeSystemCall() {
        AllocateUserMemory(UserFreeList);

        long Size = GPR[2];

        if(Size < 1) {
            System.out.print("Error, Invalid Size");
            return ERROR_INVALID_MEMORY_SIZE;
        }

        if(Size == 1) {
            Size = 2;
        }

        GPR[0] = FreeUserMemory(GPR[1], Size);

        System.out.print(MemFreeSystemCall() + GPR[0] + GPR[1] + GPR[2]);
        return GPR[0];
    }
    /*
    // Function: io_getcSystemCall
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
    static long io_getcSystemCall() {
        return StartOfInputOperation;
    }
    /*
    // Function: io_putcSystemCall
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
    static long io_putcSystemCall() {
        return StartOfOutputOperation;
    }
}
