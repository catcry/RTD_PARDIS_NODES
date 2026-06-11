use strict;
use warnings;
use Time::Local;
use NodeExtensions;
use DateTime::Duration;



########################################################################
#
# Global constants
#
########################################################################

# Diagnostic level settings
use constant DIAG_LOW => 10;
use constant DIAG_MEDIUM => 50;
use constant DIAG_HIGH => 90;
use constant FALSE => 0;
use constant TRUE => 1;
use constant FAILED              => -1;
use constant ERROR               => 0;
use constant SUCCESS             => 1;
use constant TRS_SUCCESS         => 1;
use constant TRS_ERROR           => 0;
use constant TRS_NO_DATA_FOUND   => 2;


########################################################################
#
# Global variables
#
########################################################################
my $sql;
# Flags telling which diagnostic levels are currently enabled
my $Diag_Low;
my $Diag_Medium;
my $Diag_High;


# Global structures containing configuration data
my %QUERIES;

####Node Parameters######
my $TokenDS;
my $TokenTable;
my $Renewal_Period;
my $MaxNoOfSendRetry;


my $RequestUri;
my $RemoteAddress;
my $RemotePort;
my $HeaderHost;



sub node_commit
{
    nb_diagnostic("APP", DIAG_HIGH, "node_commit()") if $Diag_High;
    sql_commit();
    nb_diagnostic("APP", DIAG_LOW, "node_commit(): returning...") if $Diag_Low;
}

sub node_control
{
    nb_diagnostic("APP", DIAG_LOW, "node_control()") if $Diag_Low;
}

sub node_end
{
    nb_diagnostic("APP", DIAG_LOW, "node_end()") if $Diag_Low;
}

sub node_flush
{
    nb_diagnostic("APP", DIAG_LOW, "node_flush()") if $Diag_Low;
}

sub node_init
{
    nb_diagnostic("APP", DIAG_LOW, "node_init(): entered the function");

    set_diagnostic_level();
	
    # cc Getting node parameters.
	
    $TokenTable = nb_get_parameter_string("Token-Table-Name");
    ## End
	

	# Load and initialise TRS library
    nb_load_library("trs");
    if (sql_initialize() != $main::TRS_SUCCESS)
    {
        nb_msg_custom($main::MSG_CRITICAL, "sql_initialize() failed.");
        nb_diagnostic("APP", DIAG_LOW, "sql_initialize() failed.");
        nb_abort();
    }	

	token_table_create($TokenTable);
	create_sql_statements();
    nb_diagnostic("APP", DIAG_LOW, "node_init(): returning...") if $Diag_Low;
}


sub node_pause
{
    nb_diagnostic("APP", DIAG_LOW, "node_pause()") if $Diag_Low;
}


# This function is called whenever the node is scheduled to be executed.
#
# Arguments:
#   None.
# Return values:
#   NB_OK if the scheduled functionality is executed successfully, NB_ERROR otherwise
#
#sub node_schedule
#{
#    nb_diagnostic("APP", DIAG_LOW, "node_schedule(): entered the function") if $Diag_Low;

#    nb_diagnostic("APP", DIAG_LOW, "node_schedule(): returning NB_OK...") if $Diag_Low;
#    return $main::NB_OK;
#}


sub node_process
{
    nb_diagnostic("APP", DIAG_HIGH, "node_process(): entered the function") if $Diag_High;

    TokenResponseHandle();
    
    nb_diagnostic("APP", DIAG_HIGH, "node_process(): returning...") if $Diag_High;
}


# This function is called for real-time nodes when they are resumed
# after having paused.
#
# Arguments:
#   None.
# Return values:
#   None.
#
sub node_resume
{
    nb_diagnostic("APP", DIAG_LOW, "node_resume()") if $Diag_Low;
}

# This function is called if an error occurs during the processing of a
# file/record, and should be used to reset the system to the point of
# the last commit.
#
# Arguments:
#   None.
# Return values:
#   None.
#
sub node_rollback
{
    nb_diagnostic("APP", DIAG_LOW, "node_rollback()") if $Diag_Low;
}



########################################################################
#
# Application-specific functions
#
########################################################################

# This function checks the current diagnostic level settings and sets
# the global variables $Diag_Low, $Diag_Medium and $Diag_High accordingly.
#
# Arguments:
#    None.
# Return values:
#    None.
#
sub set_diagnostic_level
{
    nb_diagnostic("APP", DIAG_HIGH, "set_diagnostic_level(): entered the function");

    my $diagnostic_level = nb_diagnostic_level("APP");
    nb_diagnostic("APP", DIAG_LOW, "set_diagnostic_level(): current 'APP' diagnostic level is $diagnostic_level");
    $Diag_Low = ($diagnostic_level >= DIAG_LOW);
    $Diag_Medium = ($diagnostic_level >= DIAG_MEDIUM);
    $Diag_High = ($diagnostic_level >= DIAG_HIGH);

    nb_diagnostic("APP", DIAG_HIGH, "set_diagnostic_level(): returning...");
}








## cc Functions


sub TokenResponseHandle
{
    nb_diagnostic("APP", DIAG_LOW, "TokenResponseHandle(): Entering...");

    my $EL_Status = i_get("Status");
    my $EL_Body; 
    my $EL_Access_Token;
    my $EL_Expires_in;
    my $valid_access_token_length = 10;
    my $EL_Header_Length;


    # check reponse status

    
    if ($EL_Status ne 200) {
        i_reject("Request not Successful","Response status not equal to 200");
        nb_diagnostic("APP",DIAG_HIGH,"Record Rejected Status code is not 200...");  
	    return; 
    }

    $EL_Header_Length = i_get ("Header-Content-Length");

    ## Get Body
    $EL_Body = i_get("Body");
    $EL_Body  =~ tr/\{}//d;
	$EL_Body  =~ tr/\"//d;
    my ($body1,$body2) =  split /,/, $EL_Body;
    my ($body1_key,$body1_value) = split /:/, $body1;    # body1 is access_token
    my ($body2_key,$body2_value) = split /:/, $body2;    # body2 is expires_in 
    $EL_Access_Token = $body1_value;
    $EL_Expires_in = $body2_value;

    if (!$EL_Access_Token || length ($EL_Access_Token) < $valid_access_token_length) {
        i_reject("INVALID_RESPONSE","Access Token is not valid");
	    nb_diagnostic("APP",DIAG_HIGH,"Access Token is missing or not valid...");  
	    return; 
    }

    
    ## delete from Taken Table -----------------------------------------------------------
    my $ccdquery = $QUERIES{handles}{delete_records};
    #sql_execute($ccdquery);
    
    nb_diagnostic("APP", DIAG_HIGH, "Clearing Table: executed ");
	if ( sql_execute($ccdquery) == TRS_ERROR ) {
       nb_msg1("BLN300", "Fail to execute : - " . sql_get_error_message());
       nb_abort();
     } 
    sql_close($ccdquery);


    ## Insert the Received Token to the Table --------------------------------------------
    my $cciquery = $QUERIES{handles}{insert_records};
    sql_set_parameter ($cciquery, 0, $EL_Access_Token);
    #sql_execute($cciquery);
    if ( sql_execute($cciquery) == TRS_ERROR ) {
       nb_msg1("BLN300", "Fail to execute : - " . sql_get_error_message());
       nb_abort();
     } 
    sql_close($cciquery);

}


# Create Table for Tokens in TT DS
sub token_table_create
{
	my $Table_Name = shift;
	
    # check if table exists
    sql_hide_errors();
    my $check_table = sql_execute_direct("SELECT 1 FROM ". $TokenTable."");
    my $err = sql_get_error_code();
    my $errmsg = sql_get_error_message();
	my $sql = "";
    sql_show_errors();
    
	if ( $check_table != $main::TRS_SUCCESS)
    {
        nb_diagnostic("APP", DIAG_LOW, "Table check returned code $err, message: $errmsg") if $Diag_Low;
        # cc ODBC error: Base table not found

        if ($err eq "S0002" ) 
        {
			
				$sql = "CREATE TABLE ". $TokenTable." 
				( 
					TOKEN_TIMESTAMP TIMESTAMP,
                    TOKEN VARCHAR(1024)
				)";
							
            nb_diagnostic("APP", DIAG_LOW, "Creating table: $sql") if $Diag_Low;
            if (sql_execute_direct($sql) == $main::TRS_SUCCESS) {
                sql_commit();
            }
            else {
                $errmsg = sql_get_error_message();
                nb_msg_custom($main::MSG_CRITICAL, "Table creation failed. Error: $errmsg");
                nb_diagnostic("APP", DIAG_LOW, "Table creation failed. Error: $errmsg") if $Diag_Low;
                nb_abort();
            }
        }
        else {
            nb_msg_custom($main::MSG_CRITICAL, "TRS error: $errmsg");
            nb_diagnostic("APP", DIAG_LOW, "TRS error: $errmsg") if $Diag_Low;
            nb_abort();
        }
    }
    else {
        nb_diagnostic("APP", DIAG_LOW, "Table $TokenTable already exists.") if $Diag_Low;
    }
}

#------------------------------------------------------------------------------------------------------------------------



sub create_sql_statements()
{	
	
    # Statement Delete Previous Records in Token Table

    $sql = "delete from " . $TokenTable;
    
    $QUERIES{sql}{delete_records} = $sql;
    $QUERIES{handles}{delete_records} = sql_prepare($sql);
	if ($QUERIES{handles}{delete_records} == -1) {
		nb_msg_custom($main::MSG_CRITICAL, "Node aborted. Check TRS table configuration.");
		nb_abort();
	}

    # Statement Insert into Token Table
	
	my $sql = "insert into ". $TokenTable. " 
		(TOKEN_TIMESTAMP,TOKEN)
		values (SYSDATE,?)";
		
	$QUERIES{sql}{insert_records} = $sql;
	$QUERIES{handles}{insert_records} = sql_prepare($sql);
	if ($QUERIES{handles}{insert_records} == -1) {
		nb_msg_custom($main::MSG_CRITICAL, "Node aborted. Check TRS table configuration.");
		nb_abort();
	}
}

