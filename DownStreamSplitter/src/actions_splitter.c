/************************** File Information ************************************
 * File Name                     :  RTD_ACTION_SPLITTER.c                      	*
 * Client                        :  			                          		*
 * Application                   :                              				*
 * Functionality/Description     :  Performs conversion and validation          *
 * Last modified/Latest version  :                                  			*
 * Last modified/Latest version  :  1.0.0                                       *
 *******************************************************************************/

/***********************************************************************
 *
 * Included libraries
 *
 ***********************************************************************/
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <nodebase.h>

/* Diagnostic level settings */
#define DIAG_LOW    10
#define DIAG_MEDIUM 50
#define DIAG_HIGH   90
#define DATA_LEN 84

int counter = 0;
char add[100];
char oadd[100];
char tmp[10];
char msisdn[20];
char logicId[20];
char actionName[50];
char cid[50];
char action_add[100];
char actionAbsolutePath[200];
char downstreamAbsPath[200];
char actionBlockAbsolutePath[200];
char contentAbsPath[200];
char fhandle[50];


/***********************************************************************
 *
 * Reserved functions
 *
 ***********************************************************************/

/* This function is called in the beginning when the node is starting up.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_init (void)
{
	DIAG (DIAG_LOW, "node_init(): entered ...");
	DIAG (DIAG_LOW, "node_init(): returning ...");
}

/* This function is called for every input record or for every input
 * file, if input is binary.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_process (void)
{
	DIAG (DIAG_HIGH, "node_process(): Processing Records...");
	counter=0;
	memset(add,'\0',sizeof(add));
	memset(oadd,'\0',sizeof(oadd));
	memset(tmp,'\0',sizeof(tmp));
	memset(msisdn,'\0',sizeof(msisdn));
	memset(logicId,'\0',sizeof(logicId));
	memset(actionName,'\0',sizeof(actionName));
	memset(actionAbsolutePath, '\0',sizeof(actionAbsolutePath));
	memset(downstreamAbsPath,'\0',sizeof(downstreamAbsPath));
	memset(contentAbsPath,'\0',sizeof(contentAbsPath));
	memset(actionBlockAbsolutePath,'\0',sizeof(actionBlockAbsolutePath));
	memset(cid,'\0',sizeof(cid));
	memset(fhandle,'\0',sizeof(fhandle));
	strcpy(msisdn,i_get("callingNumber"));

	// ---------------            PARDIS out Preparing           ---------------------
	i_next("__RULE_ENGINE__");
	i_enter();
	if(i_field_exists("ACTIONS")){
		
		i_next("TRIGGERED_ACTIONS");
		i_enter();
	
		while (i_next("")) {
			strcpy(actionAbsolutePath, ".__RULE_ENGINE__.ACTIONS.");
			strcpy(logicId,i_get_field_name());
			strcpy(actionName, i_get(logicId));
			strcat(actionAbsolutePath,actionName);
			strcat(actionAbsolutePath,".");
			strcpy(downstreamAbsPath,actionAbsolutePath);
			strcpy(contentAbsPath,actionAbsolutePath);
			strcat(downstreamAbsPath,"DOWNSTREAM");
			strcat(contentAbsPath,"CONTENT_ID");
			if(strcmp(i_get(downstreamAbsPath),"PARDIS") == 0 || strcmp(i_get(downstreamAbsPath),"pardis") == 0){
					nb_new_record();
					strcpy(cid,i_get(contentAbsPath));
					o_add_field("msisdn",msisdn);
					o_add_field("logic_id",logicId);
					o_add_field("action_id",actionName);
					o_add_field("content_id",cid);
					o_add_field("retry_count","0");
					nb_write_record("OUT_PARDIS");
				}
		}
		i_exit();


		// ---------------            CMS out Preparing           ---------------------
		nb_new_record();
		o_copy_input();
		o_next("__RULE_ENGINE__");
		o_enter();
		o_next("TRIGGERED_ACTIONS");
		o_enter();
		while(o_next(""))
		{
			strcpy(actionAbsolutePath, ".__RULE_ENGINE__.ACTIONS.");
			strcpy(logicId,o_get_field_name());
			strcpy(actionName, o_get(logicId));
			strcat(actionAbsolutePath,actionName);
			strcpy(actionBlockAbsolutePath,actionAbsolutePath);
			strcat(actionAbsolutePath,".");
			strcpy(downstreamAbsPath,actionAbsolutePath);
			strcat(downstreamAbsPath,"DOWNSTREAM");
			if(strcmp(o_get(downstreamAbsPath),"PARDIS") == 0 || strcmp(o_get(downstreamAbsPath),"pardis") == 0){
				
				o_delete(logicId);
				o_delete(actionBlockAbsolutePath);
			}
			
		}
		o_exit();
		nb_write_record("OUT_CMS");

		memset(oadd, '\0', sizeof(oadd));
		i_exit();
	}
	DIAG (DIAG_HIGH, "node_process(): returning...");
}


/* This function is called when the node commits after processing an
 * input file successfully.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_commit (void)
{
	DIAG (DIAG_LOW, "node_commit()");

}

/* This function is called whenever a control record is received.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_control (void)
{
	DIAG (DIAG_LOW, "node_control()");
}


/* This function is called in the end when the node is shutting down.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_end (void)
{
	DIAG (DIAG_LOW, "node_end()");
}

/* This function is called when the operator requested the flushing
 * of the steram. If the node stores any records in an internal
 * storage, all records should be retrieved from the storage and
 * written to the output in this function.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_flush (void)
{
	DIAG (DIAG_LOW, "node_flush()");
}


/* This function is called if an error occurs during the processing of a
 * file/record, and should be used to reset the system to the point of
 * the last commit.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_rollback (void)
{
	DIAG (DIAG_LOW, "node_rollback()");
}

/* This function is called whenever the node is scheduled to be executed.
 *
 * Arguments:
 *   None.
 * Return values:
 *   NB_OK if the scheduled functionality is executed successfully, NB_ERROR otherwise
 */
	void
node_schedule (void)
{
	DIAG (DIAG_LOW, "node_schedule()");
}

/* This function is called in regular intervals, about every second.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_timer (void)
{
	DIAG (DIAG_HIGH, "node_timer()");

}

/* This function is called for real-time nodes if they have to be stopped.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_pause (void)
{
	DIAG (DIAG_LOW, "node_pause()");
}

/* This function is called for real-time nodes when they are resumed
 * after having paused.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_resume (void)
{
	DIAG (DIAG_LOW, "node_resume()");
}

/* This function is called upon receival of external requests.
 *
 * Arguments:
 *   None.
 * Return values:
 *   None.
 */
	void
node_request (void)
{
	DIAG (DIAG_LOW, "node_request()");
}


/*
----------------------------------------------------------------------
AddOutData_common function
----------------------------------------------------------------------
*/


void remove_period(char *str){
	size_t len = strlen(str);
	if (len > 0 && str[len - 1] == '.')
	{
		str[len - 1] = '\0';
	}
}

//------------------------------------------------------------------------------
// --------------        cc Function - Get Block Name      ---------------------
/*
This funciton is to get the action block name in the ACTIONS block based on the
absolute path of the block.
*/
//------------------------------------------------------------------------------
char*  get_block_name(const char* absolute_path) {
	char* path = malloc(strlen(absolute_path) + 1 );
	if (path == NULL) {
		return NULL;
	}
	strcpy(path, absolute_path);
	char *last_dot = strrchr(absolute_path,'.');
	*last_dot = '\0';
	char *second_last_dot = strrchr(absolute_path,'.');
	char* result = malloc(strlen(second_last_dot + 1) +1);
	strcpy(result, second_last_dot + 1);
	free (path);
	return result;
}




/* FUNCTION:  lowercase - Function to convert a string to lowercase string
 *  * PARAMETERS:
 *   *    input   - input string whic need to be converted
 *    *    output  - the output string which is in lowercase           
 *     *  EXAMPLE: lowercase(apn,outputapn);  
 *      */
void uppercase(const char *input,char *output)
{
	int i;

	for(i = 0; i < strlen(input); i++)
	{
		if( input[i] >='a' && input[i] <='z')
		{
			output[i] = input[i] - 32;
		}
		else
		{
			output[i] = input[i];
		} 
	}
	output[i] = '\0';
}

